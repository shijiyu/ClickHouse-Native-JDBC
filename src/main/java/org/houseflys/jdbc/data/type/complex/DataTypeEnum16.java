package org.houseflys.jdbc.data.type.complex;

import org.houseflys.jdbc.data.IDataType;
import org.houseflys.jdbc.misc.Validate;
import org.houseflys.jdbc.serializer.BinaryDeserializer;
import org.houseflys.jdbc.serializer.BinarySerializer;
import org.houseflys.jdbc.stream.QuotedLexer;
import org.houseflys.jdbc.stream.QuotedToken;
import org.houseflys.jdbc.stream.QuotedTokenType;

import java.io.IOException;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataTypeEnum16 implements IDataType {

    private String name;
    private Short[] values;
    private String[] names;

    public DataTypeEnum16(String name, String[] names, Short[] values) {
        this.name = name;
        this.names = names;
        this.values = values;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int sqlTypeId() {
        return Types.VARCHAR;
    }

    @Override
    public Object defaultValue() {
        return values[0];
    }

    @Override
    public Object deserializeTextQuoted(QuotedLexer lexer) throws SQLException {
        QuotedToken token = lexer.next();
        Validate.isTrue(token.type() == QuotedTokenType.StringLiteral, "Expected String Literal.");
        return token.data();
    }

    @Override
    public void serializeBinary(Object data, BinarySerializer serializer) throws SQLException, IOException {
        Validate.isTrue(data instanceof String, "Expected String Parameter, but was " + data.getClass().getSimpleName());
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(data)) {
                serializer.writeShort(values[i]);
                return;
            }
        }
        throw new SQLException("");
    }

    @Override
    public Object deserializeBinary(BinaryDeserializer deserializer) throws SQLException, IOException {
        short value = deserializer.readShort();
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return names[i];
            }
        }
        throw new SQLException("");
    }

    @Override
    public void serializeBinaryBulk(Object[] data, BinarySerializer serializer) throws SQLException, IOException {
        for (Object datum : data) {
            serializeBinary(datum, serializer);
        }
    }

    @Override
    public Object[] deserializeBinaryBulk(int rows, BinaryDeserializer deserializer) throws SQLException, IOException {
        String[] data = new String[rows];
        for (int row = 0; row < rows; row++) {
            data[row] = (String) deserializeBinary(deserializer);
        }
        return data;
    }

    public static IDataType createEnum16Type(QuotedLexer lexer) throws SQLException {
        Validate.isTrue(lexer.next().type() == QuotedTokenType.OpeningRoundBracket);

        List<Short> enumVals = new ArrayList<Short>();
        List<String> enumNames = new ArrayList<String>();
        StringBuilder enumString = new StringBuilder("Enum16(");

        for (; ; ) {
            QuotedToken name = lexer.next(), equals = lexer.next(), number = lexer.next();
            Validate.isTrue(name.type() == QuotedTokenType.StringLiteral);
            Validate.isTrue(equals.type() == QuotedTokenType.Equals);
            Validate.isTrue(number.type() == QuotedTokenType.Number);

            String nameString = name.data();
            String numberString = number.data();
            enumVals.add(Short.valueOf(numberString));
            enumNames.add(String.valueOf(nameString));
            enumString.append("'").append(nameString).append("'").append(" = ").append(numberString);

            QuotedToken next = lexer.next();
            Validate.isTrue(next.type() == QuotedTokenType.Comma || next.type() == QuotedTokenType.ClosingRoundBracket);

            if (next.type() == QuotedTokenType.ClosingRoundBracket) {
                return new DataTypeEnum16(enumString.append(")").toString(),
                    enumNames.toArray(new String[enumNames.size()]),
                    enumVals.toArray(new Short[enumVals.size()]));
            }
            enumString.append(" , ");
        }
    }
}
