package com.spacedvocab.exception;

/**
 * Machine-readable error codes returned as {@code code} in every error response.
 * The frontend maps them to UI copy, see docs/design/06-ux-copy.md. Default messages are Vietnamese.
 */
public enum ErrorCode {

    // 400
    VALIDATION_FAILED("Dữ liệu chưa hợp lệ. Vui lòng kiểm tra lại các ô."),
    // 401
    UNAUTHORIZED("Vui lòng đăng nhập để tiếp tục."),
    BAD_CREDENTIALS("Email hoặc mật khẩu không đúng. Vui lòng thử lại."),
    TOKEN_EXPIRED("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."),
    // 403
    ACCOUNT_LOCKED("Tài khoản của bạn đang bị tạm khóa. Liên hệ quản trị viên để được hỗ trợ."),
    FORBIDDEN("Bạn không có quyền thực hiện thao tác này."),
    // 404 / 405
    NOT_FOUND("Không tìm thấy nội dung này, có thể nó đã bị xóa."),
    METHOD_NOT_ALLOWED("Thao tác này không được hỗ trợ."),
    // 409
    EMAIL_TAKEN("Email này đã được đăng ký. Bạn muốn đăng nhập?"),
    DECK_NAME_TAKEN("Bạn đã có deck với tên này. Hãy đặt tên khác."),
    CARD_WORD_TAKEN("Từ này đã có trong deck."),
    REVIEW_CONFLICT("Thẻ này vừa được chấm ở nơi khác. Hãy tải lại phiên ôn."),
    DECK_ALREADY_IN_PATH("Deck này đã là một bài trong lộ trình."),
    STAGE_TEST_ALREADY_SUBMITTED("Bài này đã nộp rồi."),
    // 422
    WRONG_CURRENT_PASSWORD("Mật khẩu hiện tại không đúng."),
    CARD_NOT_IN_LIBRARY("Thẻ này không thuộc thư viện của bạn."),
    DAILY_NEW_LIMIT_REACHED("Bạn đã học đủ số thẻ mới hôm nay. Hẹn bạn ngày mai nhé!"),
    LESSON_DECK_NOT_OFFICIAL("Bài trong lộ trình phải là deck chính thức."),
    STAGE_LOCKED("Chặng này chưa mở. Hoàn thành chặng trước hoặc làm bài vượt chặng nhé!"),
    STAGE_TEST_NOT_AVAILABLE("Chặng này không cần làm bài vượt chặng nữa."),
    STAGE_TEST_TOO_FEW_CARDS("Chặng này chưa đủ từ để làm bài vượt chặng."),
    STAGE_TEST_COOLDOWN("Nghỉ một chút rồi thử lại nhé! Bạn làm lại được sau 24 giờ kể từ lần trượt."),
    // 500
    INTERNAL_ERROR("Đã có lỗi xảy ra. Vui lòng thử lại sau.");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
