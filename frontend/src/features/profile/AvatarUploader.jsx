import { useWorkspace } from "../../hooks/useWorkspace";
import { api } from "../../services/api";
import { Avatar, Field } from "../../components/shared";

const MAX_SIDE = 512;

// Shrinks the picture in the browser. This also applies the camera's EXIF rotation, which the
// server drops when it re-encodes the image. If the browser cannot decode the file, the original
// is sent and the server decides (it validates and re-encodes every upload anyway).
async function prepare(file) {
  try {
    const bitmap = await createImageBitmap(file, {
      imageOrientation: "from-image",
    });
    const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height));
    const canvas = document.createElement("canvas");
    canvas.width = Math.max(1, Math.round(bitmap.width * scale));
    canvas.height = Math.max(1, Math.round(bitmap.height * scale));
    const context = canvas.getContext("2d");
    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    const blob = await new Promise((resolve) =>
      canvas.toBlob(resolve, "image/jpeg", 0.9),
    );
    return blob ? new File([blob], "avatar.jpg", { type: "image/jpeg" }) : file;
  } catch {
    return file;
  }
}

export function AvatarUploader() {
  const { tutor, setTutor, user, busy, run, setNotification } = useWorkspace();
  return (
    <div className="avatar-box">
      <Avatar
        name={tutor?.display_name || user.name}
        photo={tutor?.photo_url}
        large
      />
      <div className="avatar-actions">
        <Field label="Ảnh đại diện (JPEG hoặc PNG)">
          <input
            type="file"
            accept="image/jpeg,image/png"
            disabled={busy || !tutor}
            onChange={(e) => {
              const file = e.target.files[0];
              e.target.value = "";
              if (!file) return;
              run(async () => {
                const form = new FormData();
                form.append("file", await prepare(file));
                setTutor(await api("/tutor/photo", { method: "POST", body: form }));
                setNotification("Đã cập nhật ảnh đại diện.");
              });
            }}
          />
        </Field>
        {tutor?.photo_url && (
          <button
            type="button"
            className="secondary"
            disabled={busy}
            onClick={() =>
              run(async () => {
                setTutor(await api("/tutor/photo", { method: "DELETE" }));
                setNotification("Đã xóa ảnh đại diện.");
              })
            }
          >
            Xóa ảnh
          </button>
        )}
        {!tutor && (
          <p className="muted">
            Hãy lưu hồ sơ giảng dạy trước, sau đó bạn có thể thêm ảnh.
          </p>
        )}
      </div>
    </div>
  );
}
