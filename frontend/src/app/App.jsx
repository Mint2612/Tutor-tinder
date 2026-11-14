import { useEffect, useLayoutEffect, useRef } from "react";
import { WorkspaceProvider } from "./WorkspaceProvider";
import { useWorkspace } from "../hooks/useWorkspace";
import { api } from "../services/api";
import { Notice, Modal } from "../components/shared";
import { Auth } from "../features/auth/Auth";
import { TutorDetails } from "../features/tutors/TutorDetails";
import { TrialForm } from "../features/lessons/TrialForm";
import { AdminReview } from "../features/admin/AdminReview";
import { HomePage } from "../pages/HomePage";
import { StudentsPage } from "../pages/StudentsPage";
import { MatchingPage } from "../pages/MatchingPage";
import { MatchNotifications } from "../features/discovery/MatchNotifications";
import { LessonsPage } from "../pages/LessonsPage";
import { ReviewsPage } from "../pages/ReviewsPage";
import { ProfilePage } from "../pages/ProfilePage";
import { AdminPage } from "../pages/AdminPage";
import { MonitorPage } from "../pages/MonitorPage";

function Workspace() {
  const topbarRef = useRef(null);
  const {
    user,
    checking,
    page,
    error,
    busy,
    students,
    selected,
    detail,
    interest,
    review,
    notification,
    nav,
    linkValues,
    setPage,
    setDetail,
    setInterest,
    setReview,
    setQueue,
    setNotification,
    setUser,
    setRecommendations,
    clearWorkspace,
    run,
    navigate,
    loggedIn,
    logout,
  } = useWorkspace();
  useLayoutEffect(() => {
    const topbar = topbarRef.current;
    if (!topbar) return;
    const updateHeight = () => document.documentElement.style.setProperty(
      "--topbar-height", `${topbar.getBoundingClientRect().height}px`,
    );
    updateHeight();
    const observer = new ResizeObserver(updateHeight);
    observer.observe(topbar);
    return () => {
      observer.disconnect();
      document.documentElement.style.removeProperty("--topbar-height");
    };
  }, [checking]);
  // Messages sit at the top of the page, but Save buttons are often far below on long forms.
  useEffect(() => {
    if (error || notification) window.scrollTo({ top: 0, behavior: "smooth" });
  }, [error, notification]);
  if (checking)
    return (
      <main className="loading" aria-live="polite">
        Đang mở TutorMatch…
      </main>
    );
  return (
    <div className="app">
      <header className="topbar" ref={topbarRef}>
        <button
          className="brand plain"
          disabled={busy}
          onClick={() => navigate("home")}
        >
          <span className="brand-icon">t</span>TutorMatch
          <span className="brand-dot">.</span>
        </button>
        <nav aria-label="Điều hướng chính">
          {user ? (
            nav.map(([key, label]) => (
              <button
                key={key}
                className={page === key ? "nav active" : "nav"}
                disabled={busy}
                onClick={() => navigate(key)}
              >
                {label}
              </button>
            ))
          ) : (
            <span className="muted desktop">
              Tìm đúng gia sư, khơi mở tiềm năng
            </span>
          )}
        </nav>
        <div className="account">
          {user ? (
            <>
              <span className="desktop">{user.name}</span>
              <button
                className="secondary small"
                disabled={busy}
                onClick={logout}
              >
                Đăng xuất
              </button>
            </>
          ) : (
            <button
              className="secondary small"
              disabled={busy}
              onClick={() => setPage("login")}
            >
              Đăng nhập
            </button>
          )}
        </div>
      </header>
      <main className="container" aria-busy={busy}>
        <div role="alert">{error && <div className="error">{error}</div>}</div>
        <Notice>{notification}</Notice>
        {user && ["PARENT", "TUTOR"].includes(user.role) && <MatchNotifications key={user.id} />}
        {busy && (
          <div className="progress" role="status">
            Đang xử lý…
          </div>
        )}
        {page === "home" && <HomePage />}
        {["login", "register", "verify", "forgot", "reset"].includes(page) && (
          <Auth
            key={`${page}:${linkValues.challengeId || ""}`}
            page={page}
            initialValues={linkValues}
            setPage={setPage}
            run={run}
            busy={busy}
            onLogin={loggedIn}
            notify={setNotification}
            onReset={() => {
              clearWorkspace();
              setUser(null);
            }}
          />
        )}
        {user && page === "students" && <StudentsPage />}
        {user && page === "discover" && <MatchingPage key={`discover:${user.id}:${selected}`} />}
        {user && page === "interests" && <MatchingPage key={`interests:${user.id}:${selected}`} saved />}
        {user && page === "lessons" && <LessonsPage />}
        {user?.role === "PARENT" && page === "reviews" && <ReviewsPage />}
        {user && page === "profile" && <ProfilePage />}
        {user && page === "admin" && <AdminPage />}
        {user?.role === "ADMIN" && page === "monitor" && <MonitorPage />}
      </main>
      <footer>
        <span className="brand">TutorMatch.</span>
        <span>Cùng con tìm một khởi đầu tốt hơn.</span>
        <span>Thiết kế cho những kết nối có ý nghĩa.</span>
      </footer>
      {detail && (
        <Modal close={() => setDetail(null)}>
          <TutorDetails tutor={detail} />
          <button
            disabled={busy || !selected}
            onClick={() =>
              run(async () => {
                await api("/swipes", {
                  method: "POST",
                  body: {
                    studentId: selected,
                    tutorId: detail.id,
                    interested: true,
                  },
                });
                setRecommendations((list) =>
                  list.filter((r) => r.tutor.id !== detail.id),
                );
                setInterest(detail);
                setDetail(null);
              })
            }
          >
            Quan tâm & tìm hiểu học thử
          </button>
        </Modal>
      )}
      {interest && (
        <Modal close={() => setInterest(null)}>
          <TrialForm
            tutor={interest}
            student={students.find((s) => s.id === selected)}
            busy={busy}
            send={(form) =>
              run(async () => {
                await api("/lessons", {
                  method: "POST",
                  body: { ...form, studentId: selected, tutorId: interest.id },
                });
                setInterest(null);
                navigate("lessons");
                setNotification(
                  "Đã gửi yêu cầu. Kết nối sẽ được xác nhận khi gia sư chấp nhận.",
                );
              })
            }
          />
        </Modal>
      )}
      {review && (
        <Modal close={() => setReview(null)}>
          <AdminReview
            key={`${review.id}:${review.revision}`}
            tutor={review}
            busy={busy}
            run={run}
            decide={(decision) =>
              run(async () => {
                await api(`/admin/tutors/${review.id}/decision`, {
                  method: "POST",
                  body: decision,
                });
                setReview(null);
                setQueue(await api("/admin/tutors"));
                setNotification("Đã lưu kết quả xét duyệt.");
              })
            }
          />
        </Modal>
      )}
    </div>
  );
}

export default function App() {
  return (
    <WorkspaceProvider>
      <Workspace />
    </WorkspaceProvider>
  );
}
