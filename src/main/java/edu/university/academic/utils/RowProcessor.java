package edu.university.academic.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface RowProcessor<T> {
    T process(ResultSet rs) throws SQLException;
}
