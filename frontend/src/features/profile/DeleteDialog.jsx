import { useState } from "react";
import { Field, Modal } from "../../components/shared";

export function DeleteDialog({ title, description, passwordRequired = false, busy, error, close, confirm }) {
  const [confirmation, setConfirmation] = useState("");
  const [password, setPassword] = useState("");
  return <Modal close={busy ? () => {} : close} label={title}>
    <form onSubmit={(e) => { e.preventDefault(); if (confirmation === "XÓA") confirm(password); }}>
      <h2>{title}</h2>
      <p>{description}</p>
      {error && <p className="error" role="alert">{error}</p>}
      {passwordRequired && <Field label="Mật khẩu hiện tại để xác nhận xóa">
        <input type="password" autoComplete="current-password" required maxLength={72}
          value={password} onChange={(e) => setPassword(e.target.value)} disabled={busy} />
      </Field>}
      <Field label="Nhập XÓA để xác nhận">
        <input value={confirmation} onChange={(e) => setConfirmation(e.target.value)} autoComplete="off" disabled={busy} />
      </Field>
      <div className="row">
        <button type="button" className="secondary" disabled={busy} onClick={close}>Hủy</button>
        <button className="danger-button" disabled={busy || confirmation !== "XÓA" || (passwordRequired && !password)}>Xác nhận xóa</button>
      </div>
    </form>
  </Modal>;
}
