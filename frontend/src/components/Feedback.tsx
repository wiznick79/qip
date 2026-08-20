export function ErrorNotice({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <div className="notice notice-error" role="alert">
      <span aria-hidden="true">!</span>
      <p>{message}</p>
    </div>
  );
}

export function EmptyState({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="empty-state">
      <div className="empty-mark" aria-hidden="true">Q</div>
      <h3>{title}</h3>
      <p>{detail}</p>
    </div>
  );
}

export function LoadingRows({ columns = 3 }: { columns?: number }) {
  return (
    <div className="loading-rows" aria-label="Loading">
      {[0, 1, 2].map((row) => (
        <div className="loading-row" key={row}>
          {Array.from({ length: columns }, (_, column) => (
            <span key={column} />
          ))}
        </div>
      ))}
    </div>
  );
}
