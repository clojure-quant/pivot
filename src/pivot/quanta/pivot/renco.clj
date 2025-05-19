(ns pivot.quanta.pivot.pointfigure)

(defn calculatePointAndFigure [bar-time-series box-size reversal-amount]
  (loop [data bar-time-series
         current-price (first bar-time-series)
         trend :up
         result []]
    (if (empty? data)
      result
      (let [next-price (first data)]
        (cond
          (and (= trend :up) 
               (>= (- next-price current-price) box-size))
          (recur (rest data) next-price :up (conj result [:X next-price]))

          (and (= trend :down) (>= (- current-price next-price) box-size))
          (recur (rest data) next-price :down (conj result [:O next-price]))

          (and (= trend :up) (< (- current-price next-price) (- box-size)) (>= (- next-price current-price) reversal-amount))
          (recur (rest data) next-price :down (conj result [:O next-price]))

          (and (= trend :down) (< (- next-price current-price) (- box-size)) (>= (- current-price next-price) reversal-amount))
          (recur (rest data) next-price :up (conj result [:X next-price]))

          :else
          (recur (rest data) next-price trend result))))))
