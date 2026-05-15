import type { ReactNode } from "react";

type StateBlockProps = {
  title: string;
  description?: string;
  action?: ReactNode;
};

export function StateBlock({ title, description, action }: StateBlockProps) {
  return (
    <section className="state-block">
      <h2>{title}</h2>
      {description ? <p>{description}</p> : null}
      {action ? <div className="state-block__action">{action}</div> : null}
    </section>
  );
}
