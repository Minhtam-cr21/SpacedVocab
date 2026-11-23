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
    // 422
    WRONG_CURRENT_PASSWORD("Mật khẩu hiện tại không đúng."),
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
