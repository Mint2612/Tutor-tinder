import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../services/api";
import { useHashRoute } from "../hooks/useHashRoute";

import { WorkspaceContext } from "./workspace-store";
const authPages = ["login", "register", "verify", "forgot", "reset"];
const rolePages = {
  PARENT: ["students", "discover", "interests", "lessons", "profile"],
  TUTOR: ["profile", "lessons"],
  ADMIN: ["admin"],
};
const loadedPages = ["lessons", "profile", "admin", "interests"];

export function WorkspaceProvider({ children }) {
  const [user, setUser] = useState(null),
    [checking, setChecking] = useState(true);
  const [route, setPage] = useHashRoute();
  const [error, setError] = useState(""),
    [actionBusy, setActionBusy] = useState(false);
  const [students, setStudents] = useState([]),
    [selected, setSelectedState] = useState(""),
    [editing, setEditing] = useState(null);
  const [recommendations, setRecommendations] = useState([]),
    [detail, setDetail] = useState(null),
    [interest, setInterest] = useState(null);
  const [lessons, setLessons] = useState([]),
    [tutor, setTutor] = useState(null),
    [documents, setDocuments] = useState([]),
    [account, setAccount] = useState(null);
  const [queue, setQueue] = useState([]),
    [review, setReview] = useState(null),
    [interests, setInterests] = useState([]);
  const [loadedDiscovery, setLoadedDiscovery] = useState(false),
    [notification, setNotification] = useState("");
  const [refresh, setRefresh] = useState(0),
    [studentsOwnerLoaded, setStudentsOwnerLoaded] = useState(null),
    [loadedPageKey, setLoadedPageKey] = useState(null);
  const actionLock = useRef(false);
  const page =
    route.page === "home" ||
    authPages.includes(route.page) ||
    rolePages[user?.role]?.includes(route.page)
      ? route.page
      : user
        ? rolePages[user.role][0]
        : "login";
  const resourceKey = `${page}:${user?.id}:${selected}:${refresh}`;
  const loading =
    !!user && loadedPages.includes(page) && loadedPageKey !== resourceKey;
  const studentsLoading =
    user?.role === "PARENT" && studentsOwnerLoaded !== user.id;
  const busy = actionBusy || loading || studentsLoading;

  const setSelected = useCallback((id) => {
    setSelectedState(id);
    setRecommendations([]);
    setInterests([]);
    setLoadedDiscovery(false);
    setDetail(null);
    setInterest(null);
    setError("");
    setNotification("");
  }, []);
  const clearWorkspace = useCallback(() => {
    setStudents([]);
    setSelectedState("");
    setEditing(null);
    setRecommendations([]);
    setDetail(null);
    setInterest(null);
    setLessons([]);
    setTutor(null);
    setDocuments([]);
    setAccount(null);
    setQueue([]);
    setReview(null);
    setInterests([]);
    setLoadedDiscovery(false);
    setError("");
    setNotification("");
    setStudentsOwnerLoaded(null);
    setLoadedPageKey(null);
  }, []);
  const run = useCallback(async (fn) => {
    if (actionLock.current) return false;
    actionLock.current = true;
    setError("");
    setNotification("");
    setActionBusy(true);
    try {
      await fn();
      return true;
    } catch (e) {
      if (e.name !== "AbortError") setError(e.message);
      return false;
    } finally {
      actionLock.current = false;
      setActionBusy(false);
    }
  }, []);
  useEffect(() => {
    const controller = new AbortController();
    api("/auth/me", { signal: controller.signal })
      .then(setUser)
      .catch((e) => {
        if (e.name !== "AbortError" && e.status !== 401) setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setChecking(false);
      });
    return () => controller.abort();
  }, []);
  useEffect(() => {
    const expired = () => {
      clearWorkspace();
      setUser(null);
      setPage("login");
      setError("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    };
    window.addEventListener("session-expired", expired);
    return () => window.removeEventListener("session-expired", expired);
  }, [clearWorkspace, setPage]);
  useEffect(() => {
    if (!checking && page !== route.page) setPage(page);
  }, [checking, page, route.page, setPage]);
  useEffect(() => {
    if (user?.role !== "PARENT") return;
    const controller = new AbortController();
    api("/students", { signal: controller.signal })
      .then((data) => {
        setStudents(data);
        setSelectedState((current) =>
          data.some((s) => s.id === current) ? current : data[0]?.id || "",
        );
      })
      .catch((e) => {
        if (e.name !== "AbortError") setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setStudentsOwnerLoaded(user.id);
      });
    return () => controller.abort();
  }, [user]);
  useEffect(() => {
    if (!user) return;
    const controller = new AbortController();
    const get = (path) => api(path, { signal: controller.signal });
    const load = async () => {
      if (page === "lessons") setLessons(await get("/lessons"));
      else if (page === "profile") {
        setAccount(await get("/account"));
        if (user.role === "TUTOR") {
          try {
            const profile = await get("/tutor/profile");
            setTutor(profile);
            setDocuments(await get("/tutor/documents"));
          } catch (e) {
            if (e.status === 404) {
              setTutor(null);
              setDocuments([]);
            } else throw e;
          }
        }
      } else if (page === "admin") setQueue(await get("/admin/tutors"));
      else if (page === "interests")
        setInterests(
          selected ? await get(`/students/${selected}/interests`) : [],
        );
    };
    if (loadedPages.includes(page)) {
      load()
        .catch((e) => {
          if (e.name !== "AbortError") setError(e.message);
        })
        .finally(() => {
          if (!controller.signal.aborted) setLoadedPageKey(resourceKey);
        });
    }
    return () => controller.abort();
  }, [page, user, selected, refresh, resourceKey]);
  const navigate = (next) => {
    setDetail(null);
    setReview(null);
    setEditing(null);
    setError("");
    setNotification("");
    setPage(next);
    setRefresh((n) => n + 1);
  };
  const loggedIn = (account) => {
    clearWorkspace();
    setUser(account);
    setPage(rolePages[account.role][0]);
  };
  const logout = () =>
    run(async () => {
      await api("/auth/logout", { method: "POST" });
      clearWorkspace();
      setUser(null);
      setPage("home");
    });
  const discover = () =>
    run(async () => {
      if (!selected) throw new Error("Hãy tạo và chọn hồ sơ học sinh trước.");
      const data = await api(`/students/${selected}/recommendations`);
      setRecommendations(data);
      setLoadedDiscovery(true);
      setPage("discover");
    });
  const swipe = (interested) =>
    run(async () => {
      const recommendation = recommendations[0];
      if (!recommendation) return;
      await api("/swipes", {
        method: "POST",
        body: {
          studentId: selected,
          tutorId: recommendation.tutor.id,
          interested,
        },
      });
      setRecommendations((list) => list.slice(1));
      if (interested) setInterest(recommendation.tutor);
    });
  const nav =
    user?.role === "PARENT"
      ? [
          ["students", "Hồ sơ của con"],
          ["discover", "Khám phá gia sư"],
          ["interests", "Đã quan tâm"],
          ["lessons", "Lịch học thử"],
          ["profile", "Tài khoản"],
        ]
      : user?.role === "TUTOR"
        ? [
            ["profile", "Hồ sơ của tôi"],
            ["lessons", "Yêu cầu học thử"],
          ]
        : [["admin", "Duyệt gia sư"]];
  return (
    <WorkspaceContext.Provider
      value={{
        user,
        checking,
        page,
        linkValues: route.params,
        error,
        busy,
        loading,
        students,
        selected,
        editing,
        recommendations,
        detail,
        interest,
        lessons,
        tutor,
        documents,
        account,
        queue,
        review,
        interests,
        loadedDiscovery,
        notification,
        nav,
        setUser,
        setAccount,
        setPage,
        setError,
        setStudents,
        setSelected,
        setEditing,
        setRecommendations,
        setDetail,
        setInterest,
        setLessons,
        setTutor,
        setDocuments,
        setQueue,
        setReview,
        setInterests,
        setLoadedDiscovery,
        setNotification,
        clearWorkspace,
        run,
        navigate,
        loggedIn,
        logout,
        discover,
        swipe,
      }}
    >
      {children}
    </WorkspaceContext.Provider>
  );
}
