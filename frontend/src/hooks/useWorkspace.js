import { useContext } from "react";
import { WorkspaceContext } from "../app/workspace-store";

export function useWorkspace() {
  const context = useContext(WorkspaceContext);
  if (!context) throw new Error("WorkspaceProvider is required");
  return context;
}
