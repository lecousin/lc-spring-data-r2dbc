package net.lecousin.reactive.data.relational.mapping;

import java.util.function.BiFunction;

import org.springframework.data.r2dbc.core.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

public class LcReactiveDataAccessStrategy extends DefaultReactiveDataAccessStrategy {

	protected R2dbcDialect dialect;
	
	public LcReactiveDataAccessStrategy(R2dbcDialect dialect, LcMappingR2dbcConverter converter) {
		super(dialect, converter);
		this.dialect = dialect;
	}
	
	@Override
	public <T> BiFunction<Row, RowMetadata, T> getRowMapper(Class<T> typeToRead) {
		ResultMappingContext resultContext = new ResultMappingContext();
		return (row, metadata) -> ((LcMappingR2dbcConverter)getConverter()).read(typeToRead, row, metadata, resultContext);
	}
	
	public R2dbcDialect getDialect() {
		return dialect;
	}

}
