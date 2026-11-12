import { useEffect, useId, useMemo, useRef, useState } from "react";
import { searchKey } from "../lib/provinces";

// A searchable dropdown: opens with the whole list, type to filter (accents optional), arrow keys
// + Enter or a click to choose. Only a value from `options` is ever reported through onChange; anything else reports ""
// and keeps the form from being submitted. The list is not inside the <label>, so the label's
// text stays just the label.
export function Combobox({ label, options, value, onChange, required }) {
  const id = useId();
  const listId = `${id}-list`;
  const input = useRef(null);
  const [query, setQuery] = useState(value ?? "");
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  // Filter only once the user types: opening a field that already shows a value lists everything.
  const [filtering, setFiltering] = useState(false);
  const key = searchKey(query);
  const exact = options.find((o) => searchKey(o) === key);
  const selected = options.includes(value) ? value : "";
  const shown = useMemo(() => {
    if (!filtering || !key) return options;
    const starts = [];
    const contains = [];
    for (const option of options) {
      const text = searchKey(option);
      if (text.startsWith(key)) starts.push(option);
      else if (text.includes(key)) contains.push(option);
    }
    return [...starts, ...contains];
  }, [options, key, filtering]);
  const invalid = query.trim() !== "" && !exact;

  useEffect(() => {
    input.current?.setCustomValidity(
      invalid ? "Hãy chọn một mục trong danh sách." : "",
    );
  }, [invalid]);
  useEffect(() => {
    if (open)
      document
        .getElementById(`${id}-option-${active}`)
        ?.scrollIntoView?.({ block: "nearest" });
  }, [open, active, id]);

  const choose = (option) => {
    setQuery(option);
    onChange(option);
    setFiltering(false);
    setOpen(false);
  };
  const openAll = () => {
    setFiltering(false);
    setActive(Math.max(0, options.indexOf(selected)));
    setOpen(true);
  };
  return (
    <div className="field combobox">
      <label htmlFor={id}>{label}</label>
      <div className="combobox-box">
        <input
          id={id}
          ref={input}
          role="combobox"
          aria-expanded={open}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-activedescendant={
            open && shown[active] ? `${id}-option-${active}` : undefined
          }
          aria-invalid={invalid ? "true" : undefined}
          autoComplete="off"
          required={required}
          maxLength={100}
          value={query}
          onFocus={openAll}
          onClick={() => {
            if (!open) openAll();
          }}
          onChange={(e) => {
            const text = e.target.value;
            setQuery(text);
            setFiltering(true);
            setOpen(true);
            setActive(0);
            // Typing a full name (with or without accents) selects it at once.
            onChange(options.find((o) => searchKey(o) === searchKey(text)) ?? "");
          }}
          onBlur={() => {
            setOpen(false);
            setFiltering(false);
            if (exact) setQuery(exact);
          }}
          onKeyDown={(e) => {
            if (e.key === "ArrowDown") {
              e.preventDefault();
              if (!open) setOpen(true);
              else setActive((i) => Math.min(i + 1, shown.length - 1));
            } else if (e.key === "ArrowUp") {
              e.preventDefault();
              setActive((i) => Math.max(i - 1, 0));
            } else if (e.key === "Enter" && open && shown[active]) {
              e.preventDefault();
              choose(shown[active]);
            } else if (e.key === "Escape" && open) {
              e.preventDefault();
              setOpen(false);
            }
          }}
        />
        {open && (
          <ul
            id={listId}
            role="listbox"
            className="combobox-list"
            // Keep the focus in the input so a click does not trigger its blur first.
            onMouseDown={(e) => e.preventDefault()}
          >
            {shown.length ? (
              shown.map((option, i) => (
                <li
                  key={option}
                  id={`${id}-option-${i}`}
                  role="option"
                  aria-selected={option === selected}
                  className={i === active ? "active" : undefined}
                  onMouseEnter={() => setActive(i)}
                  onClick={() => choose(option)}
                >
                  {option}
                </li>
              ))
            ) : (
              <li className="combobox-empty" role="presentation">
                Không có kết quả phù hợp
              </li>
            )}
          </ul>
        )}
      </div>
      {invalid && !open && (
        <p className="field-error" role="alert">
          “{query}” không có trong danh sách. Hãy chọn một mục gợi ý.
        </p>
      )}
    </div>
  );
}
