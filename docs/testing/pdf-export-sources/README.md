# Provider export sources

Use these files to create the provider corpus for issue #325.
All three source files contain the same synthetic resume.
The candidate and company are fictional.
The email uses the reserved `.test` domain.

- `sample-resume.txt` supplies text for Google Docs and Canva.
- `sample-resume.docx` supplies a document for Word and Google Docs import.
- `sample-resume.tex` supplies the LaTeX source.

The DOCX source is an input file. It is not evidence of a Word PDF export.
If a provider changes the layout, preserve the eight source lines and record the template name.
If a PDF lacks a text layer, keep it as a separate negative control.

| Source | Export procedure | Required access |
|---|---|---|
| Google Docs | Import the DOCX or paste the text. Download as PDF. | Authenticated Google Docs editor |
| Word | Open the DOCX. Export or save as PDF. | Word desktop or authenticated Word web editor |
| Canva | Put the text in a resume design. Download PDF Standard and PDF Print. | Authenticated Canva editor |
| LaTeX | Compile `sample-resume.tex` with a recorded engine version. | A local TeX engine |
| LinkedIn | Export an approved test profile with equivalent synthetic facts through Save to PDF. | An approved test profile and authenticated desktop access |

Record the exporter, version or export date, template, export settings, and SHA-256.
Do not label browser printing, LibreOffice conversion, or generated PDF objects as a provider export.
Keep the original PDF bytes. Do not rewrite the PDF with another library.

Official export instructions: [Google Docs](https://support.google.com/docs/answer/49114),
[Word](https://support.microsoft.com/en-us/word/export-word-document-as-pdf),
[Canva](https://www.canva.com/help/download-file-types/),
and [LinkedIn](https://www.linkedin.com/help/linkedin/answer/a541960/).

Only the LaTeX export can run in this workspace without a provider account.
The temporary [Tectonic](https://tectonic-typesetting.github.io/book/latest/installation/) engine lives under `.context`.
It adds no app dependency.

```sh
tectonic --outdir /tmp sample-resume.tex
```

After export, copy each PDF into the onboarding unit-test resources and device-test assets.
Run the commands in `../pdf-import-corpus.md`.
Count a fixture as usable only when the reader preserves section boundaries and the parser recovers the expected skills and experience.
Report the numerator and denominator for each provider and Android reader.
If a provider has no fixture, report it as unmeasured.
One passing fixture is a smoke test. It does not prove support for every provider template.
