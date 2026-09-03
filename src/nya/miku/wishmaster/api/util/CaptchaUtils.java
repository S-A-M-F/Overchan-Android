/*
 * Overchan Android (Meta Imageboard Client)
 * Copyright (C) 2014-2016  miku-nyan <https://github.com/miku-nyan>
 *     
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nya.miku.wishmaster.api.util;

import java.io.File;
import java.util.Locale;

import nya.miku.wishmaster.api.models.SendPostModel;
import nya.miku.wishmaster.common.Logger;

/**
 * Утилиты для работы с капчей (имена файлов, определение ошибки "неправильная капча").
 * @author miku-nyan
 */
public class CaptchaUtils {
    private static final String TAG = "CaptchaUtils";
    
    /**
     * Максимальная длина очищенного содержимого капчи, используемого в имени файла
     */
    private static final int MAX_CONTENT_LENGTH = 64;
    
    private CaptchaUtils() {}
    
    /**
     * Очистить произвольную строку (например, введённый ответ на капчу) до допустимого имени файла.
     * Недопустимые для имени файла символы заменяются на '_', пробелы схлопываются,
     * длина ограничивается {@link #MAX_CONTENT_LENGTH}.
     * Пустая (после очистки) строка заменяется на "no_answer".
     * @param content исходная строка
     * @return безопасное имя
     */
    public static String sanitizeContent(String content) {
        if (content == null) content = "";
        content = content.trim();
        StringBuilder sb = new StringBuilder(content.length());
        boolean lastWasSpace = false;
        for (int i = 0; i < content.length(); ++i) {
            char c = content.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace && sb.length() > 0) {
                    sb.append('_');
                }
                lastWasSpace = true;
            } else if (c == '\\' || c == '/' || c == ':' || c == '*' || c == '?' || c == '"' || c == '<' ||
                    c == '>' || c == '|') {
                sb.append('_');
                lastWasSpace = false;
            } else {
                sb.append(c);
                lastWasSpace = false;
            }
        }
        String result = sb.toString();
        if (result.length() == 0) result = "no_answer";
        if (result.length() > MAX_CONTENT_LENGTH) result = result.substring(0, MAX_CONTENT_LENGTH);
        return result;
    }
    
    /**
     * Определить, является ли сообщение об ошибке постинга ошибкой "неправильная капча".
     * Для точной обработки веток известных движков (kusaba/faptcha) используется явный набор фраз,
     * для wakaba-подобных веток (iichan) - консервативное совпадение по ключевым словам.
     * @param message сообщение об ошибке (может быть null)
     * @return true, если сообщение распознано как ошибка "неправильная капча"
     */
    public static boolean isWrongCaptchaError(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase(Locale.US);
        
        // 410chan / 014chan / bulochka (kusaba / faptcha)
        if (lower.contains("incorrect faptcha entered") || lower.contains("incorrect captcha entered")) {
            return true;
        }
        
        // iichan (модифицированный wakaba) - консервативная эвристика
        boolean captchaLike = lower.contains("captcha") || lower.contains("капча");
        boolean incorrectLike = lower.contains("incorrect") || lower.contains("wrong") ||
                lower.contains("неправиль") || lower.contains("неверн");
        if (captchaLike && incorrectLike) {
            Logger.w(TAG, "Wrong captcha (best-effort heuristic): " + message);
            return true;
        }
        
        return false;
    }
    
    /**
     * Проверить, является ли указанный файл временной капчей, созданной для прикрепления к посту.
     * @param model модель отправки поста
     * @param file проверяемый файл
     * @return true, если файл - временная копия капчи, созданная опцией "добавить капчу к посту"
     */
    public static boolean isCaptchaAttachment(SendPostModel model, File file) {
        return model != null && model.captchaTempFile != null && file != null && model.captchaTempFile.equals(file);
    }
    
    /**
     * Собрать имя файла капчи, отправляемое серверу при прикреплении к посту.
     * В режиме «проезд оплачен» возвращает {@code "faptcha.png"}, иначе — {@code %capcha_content%.%file_format%}.
     * @param model модель отправки поста
     * @return имя файла (например, "abcd1234.png" или "faptcha.png")
     */
    public static String getCaptchaUploadFilename(SendPostModel model) {
        if (model != null && model.captchaAlreadyPaid) return "faptcha.png";
        return sanitizeContent(model != null ? model.captchaContent : null) + ".png";
    }
}
