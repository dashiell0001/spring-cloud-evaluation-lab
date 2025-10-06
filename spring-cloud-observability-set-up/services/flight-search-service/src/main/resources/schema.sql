DROP TABLE IF EXISTS flights;

CREATE TABLE flights (
  id SERIAL PRIMARY KEY,
  origin VARCHAR(8) NOT NULL,
  destination VARCHAR(8) NOT NULL,
  departure_date DATE NOT NULL,
  return_date DATE,
  airline VARCHAR(2) NOT NULL,
  flight_number VARCHAR(10) NOT NULL,
  cabin VARCHAR(16) NOT NULL,
  base_fare NUMERIC(12,2) NOT NULL,
  total_fare NUMERIC(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  seats_available INT NOT NULL
);
