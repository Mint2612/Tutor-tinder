import { useWorkspace } from "../../hooks/useWorkspace";
import { PageHeading } from "../../components/shared";
import { AccountCard } from "./AccountCard";

export function ParentProfile() {
  const { students, navigate, busy } = useWorkspace();
  return (
    <>
      <PageHeading
        eyebrow="TÀI KHOẢN"
        title="Thông tin của bạn."
        description="Tên hiển thị và số điện thoại dùng để liên hệ sau khi gia sư chấp nhận học thử."
      />
      <AccountCard />
      <section className="panel">
        <h2>Hồ sơ của con</h2>
        {students.length ? (
          <ul>
            {students.map((s) => (
              <li key={s.id}>
                {s.nickname} · Lớp {s.grade}
              </li>
            ))}
          </ul>
        ) : (
          <p className="muted">Chưa có hồ sơ học sinh.</p>
        )}
        <button
          className="secondary"
          disabled={busy}
          onClick={() => navigate("students")}
        >
          Quản lý hồ sơ của con
        </button>
      </section>
    </>
  );
}
