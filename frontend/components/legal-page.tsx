import { MarketingShell } from "@/components/public-site"

type Section = { title: string; paragraphs: string[] }

export function LegalPage({ eyebrow, title, intro, effectiveDate, sections }: { eyebrow: string; title: string; intro: string; effectiveDate: string; sections: Section[] }) {
  return <MarketingShell><main className="inner-page legal-page"><div className="inner-hero"><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{intro}</p><small className="legal-date">Effective {effectiveDate}</small></div><div className="legal-content">{sections.map(section => <section key={section.title}><h2>{section.title}</h2>{section.paragraphs.map(paragraph => <p key={paragraph}>{paragraph}</p>)}</section>)}</div></main></MarketingShell>
}
