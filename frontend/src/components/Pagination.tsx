interface Props {
  page: number;
  hasPrevious: boolean;
  hasNext: boolean;
  onPrevious: () => void;
  onNext: () => void;
}

export function Pagination({ page, hasPrevious, hasNext, onPrevious, onNext }: Props) {
  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" disabled={!hasPrevious} onClick={onPrevious}>
        Previous
      </button>
      <span>Page {page}</span>
      <button type="button" disabled={!hasNext} onClick={onNext}>
        Next
      </button>
    </nav>
  );
}
