(ns study.env
  (:require
   [modular.system]
   ))

(def env (modular.system/system :env))
env

(def db (modular.system/system :duckdb))

(def studio (modular.system/system :studio))
