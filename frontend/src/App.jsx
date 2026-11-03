import { useEffect, useState } from "react";
import { api } from "./lib/api";
import { money, statusText, blankStudent } from "./lib/constants";
import {
  Notice,
  Avatar,
  PageHeading,
  Empty,
  StudentPicker,
  Modal,
  Field,
} from "./components/shared";
import { Auth } from "./components/Auth";
import { StudentForm, TutorForm } from "./components/ProfileForms";
import {
  SwipeCard,
  TutorDetails,
  TrialForm,
  AdminReview,
} from "./components/Discovery";

export default function App() {
  const [user, setUser] = useState(null),
    [checking, setChecking] = useState(true),
    [page, setPage] = useState("home");
  const [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [students, setStudents] = useState([]),
    [selected, setSelected] = useState("");
  const [editing, setEditing] = useState(null),
    [recommendations, setRecommendations] = useState([]),
    [detail, setDetail] = useState(null),
    [interest, setInterest] = useState(null);
  const [lessons, setLessons] = useState([]),
    [tutor, setTutor] = useState(null),
    [documents, setDocuments] = useState([]),
    [queue, setQueue] = useState([]),
    [review, setReview] = useState(null);
  const [loadedDiscovery, setLoadedDiscovery] = useState(false),
    [notification, setNotification] = useState("");
  const run = async (fn) => {
    setError("");
    setNotification("");
    setBusy(true);
    try {
      await fn();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };
  useEffect(() => {
    api("/auth/me")
      .then(setUser)
      .catch(() => {})
      .finally(() => setChecking(false));
  }, []);
  useEffect(() => {
    if (!user) return;
    let active = true;
    if (user.role === "PARENT")
      api("/students")
        .then((data) => {
          if (active) {
            setStudents(data);
            setSelected(data[0]?.id || "");
          }
        })
        .catch((e) => {
          if (active) setError(e.message);
        });
    return () => {
      active = false;
    };
  }, [user]);
  const navigate = async (next) => {
    setPage(next);
    setDetail(null);
    setError("");
    setNotification("");
    if (next === "lessons")
      await run(async () => setLessons(await api("/lessons")));
    if (next === "tutor")
      await run(async () => {
        try {
          const p = await api("/tutor/profile");
          setTutor(p);
          setDocuments(await api("/tutor/documents"));
        } catch (e) {
          if (e.message === "Bạn chưa tạo hồ sơ gia sư") setTutor(null);
          else throw e;
        }
      });
    if (next === "admin")
      await run(async () => setQueue(await api("/admin/tutors")));
  };
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
      const r = recommendations[0];
      if (!r) return;
      await api("/swipes", {
        method: "POST",
        body: { studentId: selected, tutorId: r.tutor.id, interested },
      });
      setRecommendations((list) => list.slice(1));
      if (interested) setInterest(r.tutor);
    });
  const loggedIn = (u) => {
    setUser(u);
    setPage(u.role === "PARENT" ? "students" : "home");
  };
  const nav =
    user?.role === "PARENT"
      ? [
          ["students", "Hồ sơ của con"],
          ["discover", "Khám phá gia sư"],
          ["lessons", "Lịch học thử"],
        ]
      : user?.role === "TUTOR"
        ? [
            ["tutor", "Hồ sơ giảng dạy"],
            ["lessons", "Yêu cầu học thử"],
          ]
        : [["admin", "Duyệt gia sư"]];
  if (checking)
    return (
      <main className="loading" aria-live="polite">
        Đang mở TutorMatch…
      </main>
    );
  return (
    <div className="app">
      <header className="topbar">
        <button className="brand plain" onClick={() => setPage("home")}>
          <span className="brand-icon">t</span>TutorMatch
          <span className="brand-dot">.</span>
        </button>
        <nav aria-label="Điều hướng chính">
          {user ? (
            nav.map(([key, label]) => (
              <button
                key={key}
                className={page === key ? "nav active" : "nav"}
                onClick={() =>
                  key === "discover"
                    ? (setPage(key), setDetail(null))
                    : navigate(key)
                }
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
                onClick={() =>
                  run(async () => {
                    await api("/auth/logout", { method: "POST" });
                    setUser(null);
                    setPage("home");
                    setStudents([]);
                    setRecommendations([]);
                    setInterest(null);
                    setReview(null);
                    setTutor(null);
                  })
                }
              >
                Đăng xuất
              </button>
            </>
          ) : (
            <button
              className="secondary small"
              onClick={() => setPage("login")}
            >
              Đăng nhập
            </button>
          )}
        </div>
      </header>
      <main className="container">
        <div role="alert">{error && <div className="error">{error}</div>}</div>
        <Notice>{notification}</Notice>
        {busy && (
          <div className="progress" role="status">
            Đang xử lý…
          </div>
        )}
        {page === "home" && (
          <section className="landing">
            <div className="hero">
              <p className="eyebrow">MỖI HỌC SINH, MỘT CÁCH TỎA SÁNG</p>
              <h1>
                Đúng gia sư.
                <br />
                <em>Đúng cách học.</em>
                <br />
                Cùng con tiến xa.
              </h1>
              <p className="intro">
                Không chỉ là một người dạy giỏi. Tìm người hiểu cách con học,
                kiên nhẫn cùng con và phù hợp với gia đình bạn.
              </p>
              <div className="hero-actions">
                <button
                  onClick={() =>
                    user
                      ? user.role === "PARENT"
                        ? navigate("students")
                        : navigate(user.role === "TUTOR" ? "tutor" : "admin")
                      : setPage("register")
                  }
                >
                  {user ? "Vào không gian của bạn" : "Tìm gia sư cho con"}{" "}
                  <span>↗</span>
                </button>
                {!user && (
                  <button
                    className="text-button"
                    onClick={() => setPage("register")}
                  >
                    Tôi muốn trở thành gia sư
                  </button>
                )}
              </div>
              <div className="trust-line">
                <span>✓ Hồ sơ được duyệt</span>
                <span>✓ Gợi ý có giải thích</span>
                <span>✓ Chủ động học thử</span>
              </div>
            </div>
            <div className="hero-art" aria-hidden="true">
              <div className="orbit orbit-one"></div>
              <div className="orbit orbit-two"></div>
              <span className="art-note note-one">Một cách học phù hợp ✦</span>
              <div className="illustration">
                <div className="book">
                  <span>abc</span>
                  <span>√x</span>
                </div>
                <div className="spark">✦</div>
                <div className="art-caption">
                  Kết nối hôm nay.
                  <br />
                  <strong>Tiến bộ mỗi ngày.</strong>
                </div>
              </div>
              <span className="art-note note-two">
                Hiểu con, đồng hành cùng con
              </span>
            </div>
            <div className="steps">
              {[
                [
                  "01",
                  "Chia sẻ về con",
                  "Hồ sơ và khảo sát ngắn giúp làm rõ nhu cầu học tập.",
                ],
                [
                  "02",
                  "Khám phá người phù hợp",
                  "Lướt hồ sơ đã xác minh, đọc lý do gợi ý và tìm hiểu.",
                ],
                [
                  "03",
                  "Bắt đầu bằng học thử",
                  "Gửi lời mời. Hai bên kết nối khi gia sư chấp nhận.",
                ],
              ].map(([n, title, text]) => (
                <article key={n}>
                  <span className="step-number">{n}</span>
                  <h3>{title}</h3>
                  <p>{text}</p>
                </article>
              ))}
            </div>
          </section>
        )}
        {["login", "register", "verify", "forgot", "reset"].includes(page) && (
          <Auth
            page={page}
            setPage={setPage}
            run={run}
            busy={busy}
            onLogin={loggedIn}
            notify={setNotification}
          />
        )}
        {user && page === "students" && (
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
                      <span>
                        {s.mode === "ONLINE" ? "Trực tuyến" : s.province}
                      </span>
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
        )}
        {user && page === "discover" && (
          <>
            <PageHeading
              eyebrow="MỘT NGƯỜI THẦY. NHIỀU KHỞI ĐẦU."
              title="Gặp người đồng hành của con."
              description="Lướt để tìm hiểu. Quyền quyết định luôn ở gia đình bạn."
            />
            <div className="toolbar">
              <StudentPicker
                students={students}
                selected={selected}
                onChange={(id) => {
                  setSelected(id);
                  setRecommendations([]);
                  setLoadedDiscovery(false);
                }}
              />
              <button
                className="secondary"
                disabled={busy || !selected}
                onClick={discover}
              >
                Lấy gợi ý mới
              </button>
            </div>
            {recommendations.length ? (
              <div className="discovery">
                <aside className="discovery-guide">
                  <span className="eyebrow">
                    DÀNH CHO {students.find((s) => s.id === selected)?.nickname}
                  </span>
                  <h2>
                    Phù hợp bắt đầu
                    <br />
                    từ thấu hiểu.
                  </h2>
                  <p>
                    Chỉ những gia sư đáp ứng môn học, lịch, hình thức và ngân
                    sách của con mới xuất hiện ở đây.
                  </p>
                  <div className="guide-point">
                    ✓ Đã được xác minh danh tính
                  </div>
                  <div className="guide-point">✦ Gợi ý có lý do rõ ràng</div>
                  <p className="fineprint">
                    Mức độ phù hợp là nhận định tham khảo, không phải xác suất
                    khoa học. Học thử là cơ hội để hai bên tìm hiểu.
                  </p>
                </aside>
                <SwipeCard
                  key={recommendations[0].tutor.id}
                  recommendation={recommendations[0]}
                  busy={busy}
                  onSwipe={swipe}
                  onDetail={() =>
                    run(async () =>
                      setDetail(
                        await api(`/tutors/${recommendations[0].tutor.id}`),
                      ),
                    )
                  }
                />
                <aside className="discovery-side">
                  <span className="count">
                    {recommendations.length.toString().padStart(2, "0")}
                  </span>
                  <p>hồ sơ để khám phá</p>
                  <hr />
                  <p>
                    Kéo sang phải để quan tâm.
                    <br />
                    Kéo sang trái để bỏ qua.
                  </p>
                  <p className="fineprint">
                    Quan tâm không đồng nghĩa gia sư đã nhận lời. Bạn cần gửi
                    yêu cầu học thử.
                  </p>
                </aside>
              </div>
            ) : (
              <Empty
                title={
                  loadedDiscovery
                    ? "Bạn đã xem hết gợi ý hiện tại"
                    : "Sẵn sàng gặp gia sư phù hợp?"
                }
                text={
                  loadedDiscovery
                    ? "Chưa có thêm hồ sơ đã xác minh đáp ứng đầy đủ điều kiện. Bạn có thể điều chỉnh hồ sơ hoặc quay lại sau."
                    : "Chọn hồ sơ của con và nhấn “Lấy gợi ý mới” để bắt đầu."
                }
              />
            )}
          </>
        )}
        {user && page === "lessons" && (
          <>
            <PageHeading
              eyebrow="MỖI KẾT NỐI, MỘT CƠ HỘI"
              title="Lịch học thử"
              description="Thông tin liên hệ chỉ được mở sau khi gia sư chấp nhận."
            />
            <button
              className="secondary"
              disabled={busy}
              onClick={() => navigate("lessons")}
            >
              Cập nhật trạng thái
            </button>
            <div className="lesson-list">
              {lessons.map((l) => (
                <article className="panel lesson" key={l.id}>
                  <div className="row">
                    <div>
                      <h3>
                        {l.nickname} <span className="muted">&</span>{" "}
                        {l.display_name}
                      </h3>
                      <p>{new Date(l.proposed_at).toLocaleString("vi-VN")}</p>
                    </div>
                    <span className={`status ${l.status.toLowerCase()}`}>
                      {statusText[l.status]}
                    </span>
                  </div>
                  <p>{l.message || "Không có lời nhắn"}</p>
                  {l.status === "ACCEPTED" && (
                    <div className="contact">
                      <strong>
                        Đã kết nối · Liên hệ để thống nhất buổi học
                      </strong>
                      <p>
                        Phụ huynh: {l.parent_email} · {l.parent_phone}
                      </p>
                      <p>
                        Gia sư: {l.tutor_email} · {l.tutor_phone}
                      </p>
                    </div>
                  )}
                  {user.role === "TUTOR" && l.status === "PENDING" && (
                    <div className="row">
                      <button
                        disabled={busy}
                        onClick={() =>
                          run(async () => {
                            await api(`/lessons/${l.id}`, {
                              method: "PUT",
                              body: { status: "ACCEPTED" },
                            });
                            setLessons(await api("/lessons"));
                          })
                        }
                      >
                        Chấp nhận
                      </button>
                      <button
                        className="secondary"
                        disabled={busy}
                        onClick={() =>
                          run(async () => {
                            await api(`/lessons/${l.id}`, {
                              method: "PUT",
                              body: { status: "REJECTED" },
                            });
                            setLessons(await api("/lessons"));
                          })
                        }
                      >
                        Từ chối
                      </button>
                    </div>
                  )}
                </article>
              ))}
            </div>
            {!lessons.length && (
              <Empty
                title="Chưa có yêu cầu học thử"
                text="Các lời mời và phản hồi sẽ xuất hiện tại đây."
              />
            )}
          </>
        )}
        {user && page === "tutor" && (
          <>
            <PageHeading
              eyebrow="KHÔNG GIAN GIA SƯ"
              title="Chia sẻ cách bạn giúp học sinh tiến bộ."
              description="Hoàn thiện hồ sơ, gửi bằng chứng danh tính và chờ quản trị viên duyệt."
            />
            {tutor && (
              <div className="panel row">
                <span className="status">
                  {statusText[tutor.workflow_state]}
                </span>
                <span>{tutor.rejection_reason}</span>
              </div>
            )}
            <TutorForm
              key={tutor?.revision ?? "new"}
              initial={tutor}
              busy={busy}
              save={(form) =>
                run(async () => {
                  setTutor(
                    await api("/tutor/profile", { method: "PUT", body: form }),
                  );
                  setNotification(
                    "Đã lưu bản nháp. Hồ sơ cần được duyệt lại trước khi xuất hiện trong gợi ý.",
                  );
                })
              }
            />
            {tutor && (
              <section className="panel">
                <h2>Giấy tờ xác minh riêng tư</h2>
                <p className="muted">
                  Bằng chứng danh tính: PDF, PNG, JPEG, tối đa 5 MB mỗi tệp, 5
                  tệp. Chỉ quản trị viên có quyền đọc.
                </p>
                <ul>
                  {documents.map((d) => (
                    <li key={d.id}>
                      {d.filename}{" "}
                      {["DRAFT", "REJECTED"].includes(tutor.workflow_state) && (
                        <button
                          className="text-button"
                          disabled={busy}
                          onClick={() =>
                            run(async () => {
                              await api(`/tutor/documents/${d.id}`, {
                                method: "DELETE",
                              });
                              setDocuments(await api("/tutor/documents"));
                            })
                          }
                        >
                          Xóa
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
                {["DRAFT", "REJECTED"].includes(tutor.workflow_state) && (
                  <>
                    <Field label="Tải bằng chứng danh tính">
                      <input
                        type="file"
                        accept="application/pdf,image/png,image/jpeg"
                        disabled={busy}
                        onChange={(e) => {
                          const file = e.target.files[0];
                          if (!file) return;
                          run(async () => {
                            const form = new FormData();
                            form.append("file", file);
                            await api("/tutor/documents", {
                              method: "POST",
                              body: form,
                            });
                            setDocuments(await api("/tutor/documents"));
                          });
                          e.target.value = "";
                        }}
                      />
                    </Field>
                    <button
                      disabled={busy || !documents.length}
                      onClick={() =>
                        run(async () => {
                          setTutor(
                            await api("/tutor/submit", { method: "POST" }),
                          );
                          setNotification("Hồ sơ đã được gửi để xét duyệt.");
                        })
                      }
                    >
                      Gửi hồ sơ để duyệt
                    </button>
                  </>
                )}
              </section>
            )}
          </>
        )}
        {user && page === "admin" && (
          <>
            <PageHeading
              eyebrow="QUẢN TRỊ & TIN CẬY"
              title="Xét duyệt gia sư"
              description="Đọc bằng chứng danh tính và đánh giá hồ sơ trước khi phê duyệt."
            />
            <div className="grid">
              {queue.map((t) => (
                <article className="panel" key={t.id}>
                  <h3>{t.display_name || "Hồ sơ chưa hoàn thiện"}</h3>
                  <span className="status">{statusText[t.workflow_state]}</span>
                  <p>Hiển thị: {t.visible ? "Bật" : "Tắt"}</p>
                  <button
                    className="secondary"
                    disabled={busy}
                    onClick={() =>
                      run(async () =>
                        setReview(await api(`/admin/tutors/${t.id}`)),
                      )
                    }
                  >
                    Xem hồ sơ & giấy tờ
                  </button>
                  <button
                    className="text-button"
                    disabled={busy}
                    onClick={() =>
                      run(async () => {
                        await api(`/admin/tutors/${t.id}/visibility`, {
                          method: "PUT",
                          body: { visible: !t.visible },
                        });
                        setQueue(await api("/admin/tutors"));
                      })
                    }
                  >
                    {t.visible ? "Ẩn gia sư" : "Bật hiển thị"}
                  </button>
                </article>
              ))}
            </div>
            {!queue.length && (
              <Empty
                title="Chưa có hồ sơ gia sư"
                text="Hồ sơ mới sẽ hiển thị ở đây."
              />
            )}
          </>
        )}
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
            onClick={() => {
              setInterest(detail);
              setDetail(null);
            }}
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
                setNotification(
                  "Đã gửi yêu cầu. Kết nối sẽ được xác nhận khi gia sư chấp nhận.",
                );
                await navigate("lessons");
              })
            }
          />
        </Modal>
      )}
      {review && (
        <Modal close={() => setReview(null)}>
          <AdminReview
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
              })
            }
          />
        </Modal>
      )}
    </div>
  );
}
