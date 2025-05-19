# PIVOT

- identify and use pivot points in bar series.

- db directory contains duckdb with bar series, it is managed by git-lts


- demo
  ```
  cd demo
  clj -X:npm-install
  clj -X:compile
  clj -X:backtest
  ```
 


 renko 
 heiken ak


Heikin-Ashi (HA)
- Heikin-Ashi candlesticks are a derivative of Japanese candlesticks, but rather than using actual open, high, low, and close values, this study uses recalculated values.
- HAClose = (O+H+L+C)/4
- HAOpen = (HAOpen (previous bar) + HAClose(previous bar))/2
- HAHigh = Maximum(H, HAOpen, HAClose)
- HALow = Minimum(L, HAOpen, HAClose)