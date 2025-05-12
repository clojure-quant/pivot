(ns demo.env
  (:require
   [modular.system]))

(def env (modular.system/system :env))


(def db (modular.system/system :duckdb))

(def assets2 ["EURUSD" "GBPUSD"
              "EURJPY"
              "USDJPY" "AUDUSD" "USDCHF"
              "GBPJPY"
              "USDCAD"

              "EURGBP"
              "EURCHF"  "NZDUSD" "USDNOK"
              ;"USDZAR" 
              "USDSEK" "USDMXN"])