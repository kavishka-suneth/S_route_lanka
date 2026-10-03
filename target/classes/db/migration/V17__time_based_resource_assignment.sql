UPDATE buses
SET availability_status = 'AVAILABLE'
WHERE availability_status = 'ASSIGNED';

UPDATE drivers
SET availability_status = 'AVAILABLE'
WHERE availability_status = 'ASSIGNED';

CREATE INDEX idx_schedules_bus_assignment
    ON schedules (bus_id, travel_date, status, departure_time, arrival_time);

CREATE INDEX idx_schedules_driver_assignment
    ON schedules (driver_id, travel_date, status, departure_time, arrival_time);
