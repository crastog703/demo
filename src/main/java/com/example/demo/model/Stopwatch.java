package com.example.demo.model;

public class Stopwatch {


        private final long start;

        /**
         * Initializes a new stopwatch.
         */
        public Stopwatch() {
            start = System.currentTimeMillis();
        }


        /**
         * Returns the elapsed CPU time (in milliseconds) since the stopwatch was created.
         *
         * @return elapsed CPU time (in milliseconds) since the stopwatch was created
         */
        public double elapsedTime() {
            long now = System.currentTimeMillis();
            return (now - start) ;
        }
}
