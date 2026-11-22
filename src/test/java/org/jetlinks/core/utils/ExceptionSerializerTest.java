package org.jetlinks.core.utils;

import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.authorization.token.TokenState;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.I18nSupportException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.i18n.MessageSourceInitializer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.StaticMessageSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.*;

public class ExceptionSerializerTest {
    private static final String CODE = "error.serializer.field_rejected";

    @BeforeClass
    public static void translations() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage(CODE, Locale.CHINESE, "拒绝字段 {0}");
        messages.addMessage(CODE, Locale.ENGLISH, "Rejected field {0}");
        MessageSourceInitializer.init(messages);
    }

    @Test
    public void reasonAndArgumentsSurviveTranslationAndRepeatedHops() {
        for (Locale locale : List.of(Locale.CHINESE, Locale.ENGLISH)) {
            LocaleUtils.doWith(locale, ignored -> {
                for (I18nSupportException original : List.of(
                    new I18nSupportException.NoStackTrace(CODE, "value"),
                    new BusinessException.NoStackTrace(CODE, 409, "value"),
                    new NotFoundException.NoStackTrace(CODE, "value"),
                    new ValidationException.NoStackTrace(CODE,
                        List.of(new ValidationException.Detail("field", "invalid", "detail")), "value"),
                    new UnAuthorizedException.NoStackTrace(CODE, TokenState.expired) {
                        { setArgs(new Object[]{"value"}); }
                    },
                    new AccessDenyException.NoStackTrace("resources", Set.of("query", "save")))) {
                    I18nSupportException decoded = roundTrip(roundTrip(original));
                    assertTrue(original.getClass().getSuperclass().isInstance(decoded));
                    assertEquals(original.getI18nCode(), decoded.getI18nCode());
                    Object[] args = java.util.Arrays.stream(original.getArgs()).map(String::valueOf).toArray();
                    assertArrayEquals(args, decoded.getArgs());
                    if (CODE.equals(original.getI18nCode())) {
                        assertEquals("拒绝字段 value", decoded.getLocalizedMessage(Locale.CHINESE));
                        assertEquals("Rejected field value", decoded.getLocalizedMessage(Locale.ENGLISH));
                    }
                    if (original instanceof BusinessException) {
                        assertEquals(((BusinessException) original).getStatus(), ((BusinessException) decoded).getStatus());
                    }
                    if (original instanceof UnAuthorizedException) {
                        assertEquals(((UnAuthorizedException) original).getState(), ((UnAuthorizedException) decoded).getState());
                    }
                    if (original instanceof ValidationException) {
                        ValidationException.Detail detail = ((ValidationException) decoded).getDetails().get(0);
                        assertEquals("field", detail.getProperty());
                        assertEquals("invalid", detail.getMessage());
                        assertEquals("detail", detail.getDetail());
                    }
                }
            });
        }
    }

    @Test
    public void oldFrameWithoutCodeUsesMessageAndUnknownCodeKeepsSenderText() throws Exception {
        assertEquals("Legacy message", frame("Legacy message", "", new String[0]).getI18nCode());
        I18nSupportException decoded = frame("Remote text", "unknown.remote.reason", new String[]{"value"});
        assertEquals("unknown.remote.reason", decoded.getI18nCode());
        assertArrayEquals(new Object[]{"value"}, decoded.getArgs());
        assertEquals("Remote text", roundTrip(decoded).getLocalizedMessage(Locale.ENGLISH));
    }

    private static I18nSupportException frame(String message, String code, String[] args) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeByte(3);
        output.writeUTF(message);
        output.writeUTF(code);
        output.writeShort(args.length);
        for (String arg : args) output.writeUTF(arg);
        return (I18nSupportException) ExceptionSerializer.deserialize(new DataInputStream(
            new ByteArrayInputStream(bytes.toByteArray())), false);
    }

    private static I18nSupportException roundTrip(I18nSupportException error) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ExceptionSerializer.serialize(error, new DataOutputStream(bytes), true);
        return (I18nSupportException) ExceptionSerializer.deserialize(new DataInputStream(
            new ByteArrayInputStream(bytes.toByteArray())), true);
    }
}
