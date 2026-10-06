# LensVault Performance & Benchmarking

## Search Latency Targets

- **Target Query Latency**: `< 100 ms` for 30,000 indexed images.
- **Stretch Target**: `< 30 ms` on modern mid/flagship chipsets.
- **FTS5 Unicode61 Tokenizer**: Fast prefix and phrase matching.

## Ranking Formula

```text
Score =
    (TermsMatched / TotalTerms) * 30.0
    + (ExactPhraseMatch ? 25.0 : 0.0)
    + (FilenameMatch ? 15.0 : 0.0)
    + (IsScreenshot ? 6.0 : 0.0)
    + (AvgConfidence * 10.0)
    + (RecencyDaysBonus up to 10.0)
```

## Execution Provider Comparison (Benchmark Suite)

The app features an integrated benchmark calibration screen measuring:
- **CPU**: Portable, consistent, fallback baseline (~118 ms/image).
- **XNNPACK**: Accelerated floating point SIMD (~62 ms/image).
- **NNAPI**: Hardware NPU / DSP neural acceleration (~38 ms/image).

*Note: Accelerator performance varies by chipset vendor; the app benchmarks and selects the optimal engine automatically.*
