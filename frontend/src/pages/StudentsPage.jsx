import { useWorkspace } from "../hooks/useWorkspace";
import { api } from "../services/api";
import { money, blankStudent } from "../lib/constants";
import {
  Avatar,
  PageHeading,
  Empty,
  StudentPicker,
} from "../components/shared";
import { StudentForm } from "../features/students/StudentForm";

export function StudentsPage() {
  const {
    busy,
    students,
    selected,
    editing,
    setEditing,
    setSelected,
    setRecommendations,
    setLoadedDiscovery,
    setStudents,
    setNotification,
    run,
    discover,
  } = useWorkspace();
  return (
    <>
      <PageHeading
        eyebrow="KHÔNG GIAN PHỤ HUYNH"
        title="Hiểu con hơn, tìm đúng người."
        description="Mỗi hồ sơ có nhu cầu và danh sách gợi ý riêng."
      />
      <div className="toolbar">
        <button onClick={() => setEditing({ ...blankStudent })}>
          + Thêm hồ sơ học sinh
        </button>
        {students.length > 0 && (
          <StudentPicker
            disabled={busy}
            students={students}
            selected={selected}
            onChange={(id) => {
              setSelected(id);
              setRecommendations([]);
              setLoadedDiscovery(false);
            }}
          />
        )}
      </div>
      {editing ? (
        <StudentForm
          initial={editing}
          busy={busy}
          cancel={() => setEditing(null)}
          save={(s) =>
            run(async () => {
              const saved = await api(
                `/students${editing.id ? "/" + editing.id : ""}`,
                { method: editing.id ? "PUT" : "POST", body: s },
              );
              const all = await api("/students");
              setStudents(all);
              setSelected(saved.id);
              setEditing(null);
              setLoadedDiscovery(false);
              setRecommendations([]);
              setNotification("Đã lưu hồ sơ và sự đồng ý của phụ huynh.");
            })
          }
        />
      ) : students.length ? (
        <div className="grid">
          {students.map((s) => (
            <article className="panel student" key={s.id}>
              <Avatar name={s.nickname} />
              <h2>{s.nickname}</h2>
              <p>
                Lớp {s.grade} · {s.subjects.join(", ")}
              </p>
              <p className="muted">{s.needs}</p>
              <div className="tags">
                <span>{s.mode === "ONLINE" ? "Trực tuyến" : s.province}</span>
                <span>Đến {money(s.max_hourly_rate)}/giờ</span>
              </div>
              <button
                className="secondary"
                onClick={() =>
                  setEditing({
                    ...s,
                    budget: s.max_hourly_rate,
                    consent: false,
                  })
                }
              >
                Chỉnh sửa hồ sơ & khảo sát
              </button>
            </article>
          ))}
        </div>
      ) : (
        <Empty
          title="Bắt đầu từ câu chuyện của con"
          text="Tạo hồ sơ và hoàn thành 6 câu hỏi, khoảng 5 phút, để nhận gợi ý phù hợp."
        />
      )}
      {students.length > 0 && !editing && (
        <button
          className="wide-cta"
          disabled={busy || !selected}
          onClick={discover}
        >
          Khám phá gia sư cho{" "}
          {students.find((s) => s.id === selected)?.nickname} →
        </button>
      )}
    </>
  );
}
