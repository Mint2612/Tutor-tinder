import { useCallback, useEffect, useState } from "react";

const knownPages = new Set([
  "home",
  "login",
  "register",
  "verify",
  "forgot",
  "reset",
  "students",
  "discover",
  "interests",
  "lessons",
  "profile",
  "admin",
]);
function readRoute() {
  const [path, query = ""] = window.location.hash
    .replace(/^#\/?/, "")
    .split("?");
  const page = knownPages.has(path) ? path : "home";
  return { page, params: Object.fromEntries(new URLSearchParams(query)) };
}

export function useHashRoute() {
  const [route, setRoute] = useState(readRoute);
  useEffect(() => {
    const change = () => setRoute(readRoute());
    window.addEventListener("hashchange", change);
    return () => window.removeEventListener("hashchange", change);
  }, []);
  useEffect(() => {
    // Keep email verification/reset secrets out of the address bar after reading them.
    if (["verify", "reset"].includes(route.page) && route.params.code)
      window.history.replaceState(
        null,
        "",
        `${window.location.pathname}${window.location.search}#/${route.page}`,
      );
  }, [route]);
  const navigate = useCallback((page) => {
    window.location.hash = `/${page}`;
    setRoute({ page, params: {} });
  }, []);
  return [route, navigate];
}
