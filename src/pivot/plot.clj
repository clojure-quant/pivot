(ns pivot.plot
  (:require
   [tick.core :as t]
   [tablecloth.api :as tc]
    [tech.v3.datatype :as dtype]
   [rtable.plot.vega :as plot]))

(defn plot-pivots-blue [env ds]
  (plot/vegalite-ds
   env
   {:cols [:pivot-price :idx :n]
    :spec
    {;:$schema "https://vega.github.io/schema/vega-lite/v4.json"
     :width 800
     :height 800
     :layer [{:mark {:type "point" :color "blue"}
              :encoding {:x {:field "idx" :type "quantitative"}
                         :y {:field "pivot-price" :type "quantitative"
                             :scale {:zero false}}}}]}}
   ds))

(defn plot-pivots-n-colored
  "plots a dataset of pivot points (with different colors for different n levels)"
  [env ds]
  (plot/vegalite-ds
   env
   {:cols [:pivot-price :idx :n]
    :spec {:width 800
       :height 800
       :layer [{:mark {:type "point" :filled true}
                :encoding {:x {:field "idx" :type "quantitative"}
                           :y {:field "pivot-price" :type "quantitative"
                               :scale {:zero false}}
                           ;:color {:field "n" :type "nominal"}
                           ;:color {:field "n" :type "ordinal"
                           ;        :scale {:range ["#d4f0ff" "#74c2e1" "#1e90c7" "#1266a0" "#08306b"]}}
                           :color {:field "n" :type "ordinal"
                                   :scale {:range ["yellow" "violet" "blue" "black"]}}}}]}}
   ds))

(defn add-dt-str [ds]
  ; this is needed, because in tooltip vega moves the timezone, so it is unclear which date we really have.
  (let [to-str (fn [dt]
                 (-> dt
                     (t/truncate :minutes)
                     str))]
    (tc/add-column ds :dt-str
                   (dtype/clone (dtype/emap to-str :string (:date ds))))))

; (add-dt-str pivots)


(defn plot-pivots-n-colored-lines
  "plots a dataset of pivot points (with different colors for different n levels)
   with tooltips
   and a lines-vector with horizontal lines"
  [env ds lines]
  (plot/vegalite-ds
   env
   {:cols [:pivot-price :idx :n :date
           ; for comment:
           :pivot-type  :pivot-range  :pivot-volume
           :dt-str]
    :spec  {:width 1000
            :height 1000
            :layer (concat
                    [{:mark {:type "point" :filled true}
                      :encoding {:x {:field "idx" :type "quantitative"}
                                 :y {:field "pivot-price" :type "quantitative"
                                     :scale {:zero false}}
                                 :color {:field "n" :type "ordinal"
                                         :scale {:range ["yellow" "violet" "blue" "black"]}}
                                 :tooltip [{:field "date" :type "temporal"
                                            :format "%Y-%m-%d %H:%M:%S"}
                                           {:field "dt-str" :type "nominal"}
                                           {:field "pivot-price" :type "quantitative"}
                                           {:field "pivot-type" :type "nominal"}
                                           {:field "pivot-range" :type "quantitative"}
                                           {:field "pivot-volume" :type "quantitative"}]}}]
                    (map (fn [y-val]
                           {:mark {:type "rule" :color "gray"}
                            :encoding {:y {:datum y-val
                                           :type "quantitative"}
                                       :size {:value 1}}})
                         lines))}}
   (add-dt-str ds)))
