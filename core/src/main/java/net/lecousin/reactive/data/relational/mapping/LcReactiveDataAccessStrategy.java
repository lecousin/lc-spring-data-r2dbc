package net.lecousin.reactive.data.relational.mapping;

import java.util.function.BiFunction;

import org.springframework.data.r2dbc.core.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import net.lecousin.reactive.data.relational.model.PropertiesSourceRow;

public class LcReactiveDataAccessStrategy extends DefaultReactiveDataAccessStrategy {

	protected R2dbcDialect dialect;
	
	public LcReactiveDataAccessStrategy(R2dbcDialect dialect, LcMappingR2dbcConverter converter) {
		super(dialect, converter);
		this.dialect = dialect;
	}
	
	@Override
	public <T> BiFunction<Row, RowMetadata, T> getRowMapper(Class<T> typeToRead) {
		LcEntityReader reader = new LcEntityReader(null, (LcMappingR2dbcConverter)getConverter());
		return (row, metadata) -> reader.read(typeToRead, new PropertiesSourceRow(row, metadata));
	}
	
	public R2dbcDialect getDialect() {
		return dialect;
	}

}
