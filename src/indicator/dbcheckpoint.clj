
(ns juan.dbcheckpoint
  (:require
   [quanta.bar.db.duck.admin :as admin]
   ))


(defn checkpoint-duckdb [db] 
  (println "checkpointing duck db..")
  (admin/db-size db)
  (admin/checkpoint db)  
  (println "checkpointing done.") )


