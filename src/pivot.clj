(ns study.pivot
  (:require
   [tick.core :as t]
   [tablecloth.api :as tc]
   [missionary.core :as m]
   [tech.v3.datatype.functional :as dfn]
   [tech.v3.datatype :as dtype]
   [rtable.plot.vega :as plot]
   [quanta.bar.transform.shuffle :refer [shuffle-bar-series]]
   [quanta.studio.algo :refer [get-window-trailing]]
   [quanta.studio.safe :refer [get-trailing-bars]]
   [juan.data.asset-pairs :refer [assets2]]
   [juan.util :refer [spit-ds]]
   [study.env :refer [env ]]
   [juan.indicator.pivot :as p]))

(def calendar [:forex :m])

(def w (get-window-trailing calendar 15000 (t/instant "2025-03-18T22:00:00Z")))

w

(def dt (t/instant "2025-04-21T22:00:00Z"))

(def opts {:asset "EURUSD"
           :calendar [:forex :m]
           :trailing-n 50000
           :dt (t/instant "2025-04-21T22:00:00Z")})

(def bar-ds (m/? (get-trailing-bars env opts dt)))

bar-ds

(defn bars-asset [asset]
  (let [opts {:asset asset
              :calendar [:forex :m]
              :trailing-n 50000
              :dt (t/instant "2025-04-21T22:00:00Z")}]
    (m/? (get-trailing-bars env opts dt))))

(bars-asset "EURUSD")
(bars-asset "USDJPY")

(def spec-ds
  {;:$schema "https://vega.github.io/schema/vega-lite/v4.json"
   :width 800
   :height 800
   :layer [{:mark {:type "point" :color "blue"}
            :encoding {:x {:field "idx" :type "quantitative"}
                       :y {:field "pivot-high" :type "quantitative"
                           :scale {:zero false}}}}
           {:mark {:type "point" :color "red"}
            :encoding {:x {:field "idx" :type "quantitative"}
                       :y {:field "pivot-low" :type "quantitative"
                           :scale {:zero false}}}}]})

(defn add-idx [ds]
  (tc/add-column ds :idx (range (tc/row-count ds))))

(defn vegalite2 [ds]
  (plot/vegalite-ds env {:cols [:pivot-low :pivot-high :idx]
                         :spec spec-ds} ds))


(defn load-one [{:keys [asset shuffle?]}]
  (let [bars (bars-asset asset)]
    (if shuffle?
      (shuffle-bar-series bars)
      bars)))

(defn calc-one [{:keys [ds n]}]
  (-> ds
      (p/pivots {:n n :debug? false}) ; 4 hours (in both ways)
      (tc/select-columns [:date
                          :idx
                       ;:low 
                       ;:tlow
                          :pivot-low
                       ;:high 
                       ;:thigh 
                          :pivot-high])
      ;(add-idx)
      ))

(let [ds (load-one {:asset "GBPJPY" :shuffle? false})]
  (calc-one {:ds ds
             :n 240}))


(defn calc-multi-window [{:keys [asset windows shuffle?]}]
  (let [ds (load-one {:asset asset :shuffle? shuffle?})
        calc-window (fn [n]
                      (->
                       (calc-one {:ds ds :n n})
                       (tc/add-column :n n)))]
    (->> (map calc-window windows)
         (apply tc/concat))))

(calc-multi-window
 {:asset "GBPJPY"
  :windows [240 480 720 1440
             ;4h 8h  12h 24h
            ]})

(calc-multi-window
 {:asset "GBPJPY"
  :windows [240 480 720 1440
             ;4h 8h  12h 24h
            ]
  :shuffle? true})




(tc/add-column ds :idx (range (tc/row-count ds)))

(->  ;"USDJPY"
     ;"USDCAD"
 "GBPJPY"
 (bars-asset)
 (p/pivots {:n 240 :debug? false}) ; 4 hours (in both ways)
 (tc/select-columns [:date
                     :low :tlow :pivot-low
                     :high :thigh :pivot-high])
 (add-idx)

 ;(spit-ds "pivots")
 (vegalite2)
 tap>)


(def spec-multiple-ds
  {:width 800
   :height 800
   :layer [{:mark {:type "point" :filled true}
            :encoding {:x {:field "idx" :type "quantitative"}
                       :y {:field "pivot-high" :type "quantitative"
                           :scale {:zero false}}
                       ;:color {:field "n" :type "nominal"}
                       ;:color {:field "n" :type "ordinal"
                       ;        :scale {:range ["#d4f0ff" "#74c2e1" "#1e90c7" "#1266a0" "#08306b"]}}
                       :color {:field "n" :type "ordinal"
                               :scale {:range ["yellow" "violet" "blue" "black"]}}}}
           {:mark {:type "point" :filled true}
            :encoding {:x {:field "idx" :type "quantitative"}
                       :y {:field "pivot-low" :type "quantitative"
                           :scale {:zero false}}
                       ;:color {:field "n" :type "nominal"}
                       ;:color {:field "n" :type "ordinal"
                       ;        :scale {:range ["#d4f0ff" "#74c2e1" "#1e90c7" "#1266a0" "#08306b"]}}
                       :color {:field "n" :type "ordinal"
                               :scale {:range ["yellow" "violet" "blue" "black"]}}}}]})

(def spec-multiple-ds2
  {:width 800
   :height 800
   :layer [{:mark {:type "point"}
            :encoding {:x {:field "idx" :type "quantitative"}
                       :y {:field "pivot-high" :type "quantitative"
                           :scale {:zero false}}
                       :color {:field "n" :type "quantitative"
                               :scale {:scheme "viridis"}}}}
           {:mark {:type "point"}
            :encoding {:x {:field "idx" :type "quantitative"}
                       :y {:field "pivot-low" :type "quantitative"
                           :scale {:zero false}}
                       :color {:field "n" :type "quantitative"
                               :scale {:scheme "viridis"}}}}]})

(defn vegalite-multiple [ds]
  (plot/vegalite-ds env {:cols [:pivot-low :pivot-high :idx :n]
                         :spec spec-multiple-ds} ds))


(-> (calc-multi-window
     {:asset "NZDUSD"
      :windows [240 480 720 1440
                     ;4h 8h  12h 24h
                ]
      :shuffle? true ; false
      })
    vegalite-multiple
    tap>)


(defn vegalite-multiple-pivots [ds pivots]
  (plot/vegalite-ds
   env
   {:cols [:pivot-low :pivot-high :idx :n :date]
    :spec  {:width 1000
            :height 1000
            :layer (concat
                    [{:mark {:type "point" :filled true}
                      :encoding {:x {:field "idx" :type "quantitative"}
                                 :y {:field "pivot-high" :type "quantitative"
                                     :scale {:zero false}}
                                 :color {:field "n" :type "ordinal"
                                         :scale {:range ["yellow" "violet" "blue" "black"]}}
                                 :tooltip [{:field "date" :type "temporal"}
                                           {:field "pivot-high" :type "quantitative"}
                                           {:field "pivot-low" :type "quantitative"}]}}
                     {:mark {:type "point" :filled true}
                      :encoding {:x {:field "idx" :type "quantitative"}
                                 :y {:field "pivot-low" :type "quantitative"
                                     :scale {:zero false}}
                                 :color {:field "n" :type "ordinal"
                                         :scale {:range ["yellow" "violet" "blue" "black"]}}
                                 :tooltip [{:field "date" :type "temporal"}
                                           {:field "pivot-high" :type "quantitative"}
                                           {:field "pivot-low" :type "quantitative"}]}}]
                    (map (fn [y-val]
                           {:mark {:type "rule" :color "gray"}
                            :encoding {:y {:datum y-val
                                           :type "quantitative"}
                                       :size {:value 1}}})
                         pivots))}}
   ds))


(-> (calc-multi-window
     {:asset "NZDUSD"
      :windows [240 480 720 1440
                     ;4h 8h  12h 24h
                ]
      :shuffle? true ; false
      })
    (vegalite-multiple-pivots
     [0.58 0.57])
    tap>)

(defn lines [middle step nr]
  (->> (concat
        (map  #(+ middle (* step %)) (range nr))
        (map  #(- middle (* step %)) (range nr)))
       (into [])))

(lines 189.0 1.5 7)


(-> (calc-multi-window
     {:asset "GBPJPY"
      :windows [240 480 720 1440
               ;4h 8h  12h 24h
                ]
      :shuffle? false})
    (vegalite-multiple-pivots
     ;[189.0]
     ;(lines 189.0 1.5 6)
     (lines 189.0 0.75 12))
    tap>)





(-> (calc-multi-window
     {:asset "GBPJPY"
      :windows [240 480 720 1440
                   ;4h 8h  12h 24h
                ]
      :shuffle? false})
    (tc/select-rows  (fn [row]
                       (t/> (t/instant "2025-04-10T23:30:00Z")
                            (:date row)
                            (t/instant "2025-04-03T23:30:00Z")
                            ))))

(> 5 4 3)

(-> (calc-multi-window
     {:asset "EURGBP"
      :windows [240 480 720 1440
               ;4h 8h  12h 24h
                ]
      :shuffle? false})
    (vegalite-multiple-pivots
     ;[]
     ;(lines 189.0 1.5 4)
     ;(lines 0.852 0.007 4)
     (lines 0.852 0.0035 8))
    tap>)


(-> (calc-multi-window
     {:asset "NZDUSD"
      :windows [240 480 720 1440
               ;4h 8h  12h 24h
                ]
      :shuffle? false})
    (vegalite-multiple-pivots
     ;[]

     (lines 0.571 0.004 8))
    tap>)


(-> (calc-multi-window
     {:asset "EURUSD"
      :windows [240 480 720 1440
               ;4h 8h  12h 24h
                ]
      :shuffle? false})
    (vegalite-multiple-pivots
     ;[]

     (lines 1.105 0.005 10))
    tap>)



(-> (calc-multi-window
     {:asset "USDCAD"
      :windows [240 480 720 1440
               ;4h 8h  12h 24h
                ]
      :shuffle? false})
    (vegalite-multiple-pivots
     ;[]

     ;(lines 1.427 0.01 5)
     (lines 1.441 0.014 5))
    tap>)