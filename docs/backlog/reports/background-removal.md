# Cutting the background out of an item photo, for free (WR-034)

Report only: no code, no dependency, nothing installed. Written 27 November 2026 from a cloud run.

**Goal.** Items are shown as cut-outs on a light backing (`docs/design/DESIGN.md`, «Items look like cut-out photos»).
We need a way to cut out the background with **no paid API, no key, cost 0**, and — intent 01 — the photo must not
leave Aika's phone or our own server (photos are personal data).

**What could be checked from here.** Reachable: GitHub (code, licence files, READMEs), Apple's documentation JSON,
the npm registry. **Not reachable** from this environment (proxy refused): `huggingface.co` (model cards, so the
licences of RMBG-1.4 / BiRefNet weights / MODNet weights), `developers.google.com` (ML Kit docs and terms),
`dl.google.com` (ML Kit Maven). Facts from those are marked **(not checked here)** — they are from memory and must
be re-read before a decision. Nothing was run: quality and speed are from the projects' own descriptions, not measured.

## Comparison

| # | Option | Where it runs | What it needs | Licence: code / model weights | Quality on clothes | Speed | Fit with Expo | Cost |
|---|---|---|---|---|---|---|---|---|
| 1 | Apple Vision subject lifting, `VNGenerateForegroundInstanceMaskRequest` (iOS 17) / `GenerateForegroundInstanceMaskRequest` (iOS 18) | Device (iPhone) | iOS/iPadOS 17.0+ (macOS 14+) [A1][A2]; model ships with the OS, 0 MB in the app | System API, proprietary but free to use in any app; no separate weights licence | Designed for "noticeable objects" generally [A1]; good on a garment laid flat on a contrasting surface; white on white and a hanger in the shot are weak spots (not checked here) | Fast, on the Neural Engine, well under a second (not checked here) | **Not in Expo Go.** Needs a native module → dev build. An MIT wrapper exists: `@six33/react-native-bg-removal` 1.3.4 (iOS 17 Vision + Android ML Kit) [N1] | 0 |
| 2 | Google ML Kit Subject Segmentation | Device (Android) | Google Play services; model downloaded on demand by Play services (not checked here); sample app uses `play-services-mlkit-subject-segmentation:16.0.0-beta1`, still **beta** [G1]; Android 7 / API 24+ (not checked here) | Sample code Apache-2.0 [G2]; the SDK itself is closed and free under the ML Kit terms (not checked here); no separate weights licence | Segments foreground subjects (people, objects); clothes laid flat should work; white on white weak (not checked here) | Fast on device (not checked here) | **Not in Expo Go.** Native module → dev build; same wrapper as #1 [N1] | 0 |
| 3 | `rembg` with open models, as a separate process next to our Java server (CLI or its own HTTP server `rembg s`) | Our server | Python 3.11–3.13, `onnxruntime` (CPU fine, GPU optional); Docker image ~1.6 GB CPU [R1]; model files: `u2net` ~170 MB, `silueta` 43 MB, `isnet-general-use`, `birefnet-general` / `-lite`, `u2net_cloth_seg` [R1] | rembg code **MIT** [R2]. Weights: U²-Net **Apache-2.0** [R3]; IS-Net (DIS) **Apache-2.0** [R4]; BiRefNet code **MIT** [R5] (weights card on HF says MIT — not checked here); `u2net_cloth_seg` code MIT [R6] but trained on a Kaggle competition set whose terms may limit use (not checked here). **Trap: rembg's default model `bria-rmbg` (RMBG-2.0) needs a paid agreement for commercial use** [R1] — always pass `-m` | BiRefNet / IS-Net give soft, well-shaped edges; U²-Net / silueta firmer, blockier edges [R1]; folds fine; white on white weak for all. `u2net_cloth_seg` parses clothes *on a person* into upper/lower/full body [R6] — not for a flat lay | Depends on the machine. BiRefNet runs at 1024×1024 and is slow on CPU (seconds per photo); u2net faster [R1] (not measured) | Phone uploads the photo to **our** server; works with Expo Go (plain HTTP). Adds a Python runtime beside Java | 0 (our machine) |
| 4 | Browser / JS: `@imgly/background-removal` (browser) and `-node` | Device in a browser, or Node on our server | WASM / WebGPU via onnxruntime-web; model files downloaded at first run (tens of MB) | **AGPL-3.0** for the code [I1][I2]; other licences only by contacting the vendor [I2]. AGPL on a server we expose = we must offer our source | ISNet-based (not checked here); fine on products | Seconds on CPU/WASM | Browser package does **not** run in React Native (no WASM/DOM); only Expo **web** or a WebView | 0, but AGPL |
| 5 | Browser / JS: Transformers.js `background-removal` pipeline [T1] | Device in a browser (WASM CPU, or WebGPU), or Node | `@huggingface/transformers`; model of choice, quantised `q8` by default on WASM [T1] | Library **Apache-2.0** [T2]. Weights depend on the model: `briaai/RMBG-1.4` is **non-commercial** (not checked here); MODNet **Apache-2.0** [M1] but it is a *portrait* matting model; BiRefNet ONNX MIT (not checked here) | As the chosen model | Seconds; WebGPU still experimental in many browsers [T1] | Same as #4: Expo web or WebView only, not Expo Go native | 0 |
| 6 | One more: `transparent-background` (InSPyReNet) as a separate process on our server | Our server | Python + PyTorch, CPU or GPU; weights downloaded at first run (not checked here) | Code **MIT** [P1]; weights licence not stated in the files read (not checked here) | Strong edges on salient objects (not checked here) | Slow on CPU (not checked here) | Like #3: upload to our server | 0 |

## Notes per option

- **On the phone (1 + 2)** is the only way the photo *never leaves the phone* — the strongest fit for intent 01 —
  and costs nothing on the server. The price: we leave Expo Go for a **dev build** (EAS or local), and we get two
  engines with two different qualities. iPhones before iOS 17 get nothing. The MIT wrapper [N1] is small and
  recent (January 2026) but from a single vendor; its "API fallback" for old iOS would send the photo to a paid
  service — we would not use that part.
- **On our server (3, 6)** gives one engine, the same result on every phone, works in Expo Go, and lets us choose an
  open model (IS-Net Apache-2.0 or BiRefNet MIT). The photo goes to *our* machine only, which intent 01 allows
  ("stay on her machine"); it must not be stored longer than needed. The price: a Python process and ~1–2 GB next to
  the Java server — a new runtime dependency, so an ADR.
- **In the browser (4, 5)** does not fit a native Expo app; only useful if the web build becomes the main client.
  `@imgly` is AGPL — avoid.
- **Licence trap**: in every "free" tool the default model is often the non-commercial one (rembg → RMBG-2.0;
  many Transformers.js demos → RMBG-1.4). Pin the model by name.
- **Clothes specifically**: no option is trained for a flat lay of one item. Ask Aika to shoot on a contrasting,
  plain surface; white on white will need a manual touch-up (a brush/eraser) in any option.

## Recommendation

**Option A (recommended): on the phone first — Apple Vision on iOS 17+ and ML Kit on Android — via a dev build.**
Photo never leaves the phone, 0 cost, fastest. A later task picks the wrapper or writes a small Expo module, with an
ADR (new native dependency, leaving Expo Go). Phones that cannot do it: the item keeps its drawing (as now).

**Option B: on our server — `rembg` with `isnet-general-use` (Apache-2.0) or `birefnet-general-lite` (MIT), pinned,
as a separate process.** One quality for all phones, stays in Expo Go; costs a Python runtime beside Java (ADR).

**Option C: A + B** — phone when it can, server as a fallback for old phones. Most coverage, most to maintain.

Before deciding: re-read the pages this run could not reach (ML Kit terms and requirements, the Hugging Face model
cards for BiRefNet and IS-Net weights), and try 10 of Aika's own item photos — on her phone or her machine, never in
the cloud — with A and B.

Решение — за Aika.

## Sources

- [A1] Apple, `VNGenerateForegroundInstanceMaskRequest` — https://developer.apple.com/documentation/vision/vngenerateforegroundinstancemaskrequest (read via its JSON: iOS 17.0, macOS 14.0)
- [A2] Apple, `GenerateForegroundInstanceMaskRequest` — https://developer.apple.com/documentation/vision/generateforegroundinstancemaskrequest (iOS 18.0)
- [G1] ML Kit samples, `android/vision-quickstart/app/build.gradle` — https://github.com/googlesamples/mlkit
- [G2] ML Kit samples licence (Apache-2.0) — https://github.com/googlesamples/mlkit/blob/master/LICENSE
- ML Kit docs https://developers.google.com/ml-kit/vision/subject-segmentation — **could not be reached from here**
- [N1] `@six33/react-native-bg-removal` on npm (MIT, 1.3.4) — https://www.npmjs.com/package/@six33/react-native-bg-removal
- [R1] rembg README (models, sizes, default model and its licence note) — https://github.com/danielgatis/rembg
- [R2] rembg licence (MIT) — https://github.com/danielgatis/rembg/blob/main/LICENSE.txt
- [R3] U²-Net licence (Apache-2.0) — https://github.com/xuebinqin/U-2-Net/blob/master/LICENSE
- [R4] DIS / IS-Net licence (Apache-2.0) — https://github.com/xuebinqin/DIS/blob/main/LICENSE.md
- [R5] BiRefNet licence (MIT) — https://github.com/ZhengPeng7/BiRefNet/blob/main/LICENSE
- [R6] cloth-segmentation (MIT; trained on a Kaggle fashion set) — https://github.com/levindabhi/cloth-segmentation
- [I1] `@imgly/background-removal-js` licence (AGPL-3.0) — https://github.com/imgly/background-removal-js/blob/main/LICENSE.md
- [I2] its README, licence section — https://github.com/imgly/background-removal-js
- [T1] Transformers.js README (background-removal pipeline, WASM/WebGPU, dtypes) — https://github.com/huggingface/transformers.js
- [T2] Transformers.js licence (Apache-2.0) — https://github.com/huggingface/transformers.js/blob/main/LICENSE
- [M1] MODNet licence (Apache-2.0) — https://github.com/ZHKKKe/MODNet/blob/master/LICENSE
- [P1] transparent-background licence (MIT) — https://github.com/plemeri/transparent-background/blob/main/LICENSE
- Hugging Face model cards (RMBG-1.4, RMBG-2.0, BiRefNet) — **could not be reached from here**
