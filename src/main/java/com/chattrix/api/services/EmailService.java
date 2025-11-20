package com.chattrix.api.services;

import com.chattrix.api.config.MailConfig;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    private MailConfig mailConfig;

    private Session mailSession;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", String.valueOf(mailConfig.isSmtpAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(mailConfig.isStarttlsEnable()));
        props.put("mail.smtp.host", mailConfig.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(mailConfig.getSmtpPort()));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        // Check if SMTP credentials are configured
        if (mailConfig.isConfigured()) {
            mailSession = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailConfig.getUsername(), mailConfig.getPassword());
                }
            });
            LOGGER.info("Email service initialized with SMTP configuration");
        } else {
            LOGGER.warning("SMTP credentials not configured. Email sending will be simulated (logged to console).");
            LOGGER.warning("Please configure mail settings in application.properties or environment variables");
        }
    }

    /**
     * Generate a 6-digit OTP
     */
    public String generateOTP() {
        int otp = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Send verification email with OTP
     */
    public void sendVerificationEmail(String email, String fullName, String otp) {
        String subject = "Xác Thực Email - Chattrix";
        String htmlContent = buildVerificationEmailTemplate(fullName, otp);
        sendEmail(email, subject, htmlContent);
    }

    /**
     * Send password reset email with OTP
     */
    public void sendPasswordResetEmail(String email, String fullName, String otp) {
        String subject = "Đặt Lại Mật Khẩu - Chattrix";
        String htmlContent = buildPasswordResetEmailTemplate(fullName, otp);
        sendEmail(email, subject, htmlContent);
    }

    /**
     * Send welcome email after successful registration
     */
    public void sendWelcomeEmail(String email, String fullName) {
        String subject = "Chào Mừng Đến Với Chattrix!";
        String htmlContent = buildWelcomeEmailTemplate(fullName);
        sendEmail(email, subject, htmlContent);
    }

    /**
     * Core method to send email
     */
    private void sendEmail(String to, String subject, String htmlContent) {
        if (mailSession == null) {
            // Simulate email sending by logging to console
            LOGGER.info("==============================================");
            LOGGER.info("SIMULATED EMAIL (SMTP not configured)");
            LOGGER.info("To: " + to);
            LOGGER.info("Subject: " + subject);
            LOGGER.info("----------------------------------------------");
            LOGGER.info(htmlContent);
            LOGGER.info("==============================================");
            return;
        }

        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(mailConfig.getFromAddress(), mailConfig.getFromName()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            // Encode subject with UTF-8 to support Vietnamese characters
            message.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            LOGGER.info("Email sent successfully to: " + to);
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Failed to encode email subject: " + subject, e);
            throw new RuntimeException("Failed to encode email subject", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send email to: " + to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Build HTML template for verification email - Shadcn UI inspired (Black & White)
     */
    private String buildVerificationEmailTemplate(String fullName, String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif; background-color: #fafafa;\">" +
                "  <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse;\">" +
                "    <tr>" +
                "      <td align=\"center\" style=\"padding: 40px 20px;\">" +
                "        <!-- Main Container -->" +
                "        <table role=\"presentation\" style=\"max-width: 600px; width: 100%; border-collapse: collapse; background-color: #ffffff; border: 1px solid #e5e7eb; border-radius: 12px; box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);\">" +
                "          <!-- Header -->" +
                "          <tr>" +
                "            <td style=\"padding: 48px 40px 40px; text-align: center; border-bottom: 1px solid #e5e7eb;\">" +
                "              <h1 style=\"margin: 0; font-size: 28px; font-weight: 700; color: #000000; letter-spacing: -0.025em;\">Chattrix</h1>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Content -->" +
                "          <tr>" +
                "            <td style=\"padding: 48px 40px;\">" +
                "              <!-- Title -->" +
                "              <h2 style=\"margin: 0 0 24px; font-size: 24px; font-weight: 600; color: #000000; letter-spacing: -0.02em;\">Xác Thực Email</h2>" +
                "              " +
                "              <!-- Greeting -->" +
                "              <p style=\"margin: 0 0 16px; font-size: 16px; line-height: 1.6; color: #525252;\">Xin chào <strong style=\"color: #000000; font-weight: 600;\">" + fullName + "</strong>,</p>" +
                "              " +
                "              <!-- Message -->" +
                "              <p style=\"margin: 0 0 40px; font-size: 15px; line-height: 1.7; color: #525252;\">Cảm ơn bạn đã đăng ký tài khoản Chattrix! Vui lòng sử dụng mã xác thực bên dưới để xác thực địa chỉ email của bạn:</p>" +
                "              " +
                "              <!-- OTP Box -->" +
                "              <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse; margin-bottom: 40px;\">" +
                "                <tr>" +
                "                  <td style=\"padding: 40px 32px; text-align: center; background: linear-gradient(to bottom, #fafafa, #f9fafb); border: 2px solid #e5e7eb; border-radius: 12px;\">" +
                "                    <div style=\"font-size: 48px; font-weight: 700; color: #000000; letter-spacing: 16px; font-family: 'Courier New', Consolas, monospace; line-height: 1.2;\">" + otp + "</div>" +
                "                  </td>" +
                "                </tr>" +
                "              </table>" +
                "              " +
                "              <!-- Expiry Warning -->" +
                "              <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse; margin-bottom: 24px;\">" +
                "                <tr>" +
                "                  <td style=\"padding: 20px 24px; background-color: #fafafa; border-left: 4px solid #000000; border-radius: 6px;\">" +
                "                    <p style=\"margin: 0; font-size: 14px; line-height: 1.6; color: #000000; font-weight: 600;\">⏰ Mã này sẽ hết hạn sau 15 phút</p>" +
                "                  </td>" +
                "                </tr>" +
                "              </table>" +
                "              " +
                "              <!-- Help Text -->" +
                "              <p style=\"margin: 0; font-size: 14px; line-height: 1.6; color: #737373;\">Nếu bạn không yêu cầu xác thực này, vui lòng bỏ qua email này.</p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style=\"padding: 40px; text-align: center; border-top: 1px solid #e5e7eb; background-color: #fafafa;\">" +
                "              <p style=\"margin: 0 0 8px; font-size: 13px; line-height: 1.5; color: #a3a3a3;\">Đây là email tự động, vui lòng không trả lời email này.</p>" +
                "              <p style=\"margin: 0; font-size: 12px; color: #d4d4d4;\">© 2025 Chattrix. All rights reserved.</p>" +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }

    /**
     * Build HTML template for password reset email - Shadcn UI inspired (Black & White)
     */
    private String buildPasswordResetEmailTemplate(String fullName, String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif; background-color: #fafafa;\">" +
                "  <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse;\">" +
                "    <tr>" +
                "      <td align=\"center\" style=\"padding: 40px 20px;\">" +
                "        <!-- Main Container -->" +
                "        <table role=\"presentation\" style=\"max-width: 600px; width: 100%; border-collapse: collapse; background-color: #ffffff; border: 1px solid #e5e7eb; border-radius: 12px; box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);\">" +
                "          <!-- Header -->" +
                "          <tr>" +
                "            <td style=\"padding: 48px 40px 40px; text-align: center; border-bottom: 1px solid #e5e7eb;\">" +
                "              <h1 style=\"margin: 0; font-size: 28px; font-weight: 700; color: #000000; letter-spacing: -0.025em;\">Chattrix</h1>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Content -->" +
                "          <tr>" +
                "            <td style=\"padding: 48px 40px;\">" +
                "              <!-- Title -->" +
                "              <h2 style=\"margin: 0 0 24px; font-size: 24px; font-weight: 600; color: #000000; letter-spacing: -0.02em;\">Đặt Lại Mật Khẩu</h2>" +
                "              " +
                "              <!-- Greeting -->" +
                "              <p style=\"margin: 0 0 16px; font-size: 16px; line-height: 1.6; color: #525252;\">Xin chào <strong style=\"color: #000000; font-weight: 600;\">" + fullName + "</strong>,</p>" +
                "              " +
                "              <!-- Message -->" +
                "              <p style=\"margin: 0 0 40px; font-size: 15px; line-height: 1.7; color: #525252;\">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Vui lòng sử dụng mã bên dưới để đặt lại mật khẩu:</p>" +
                "              " +
                "              <!-- OTP Box -->" +
                "              <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse; margin-bottom: 40px;\">" +
                "                <tr>" +
                "                  <td style=\"padding: 40px 32px; text-align: center; background: linear-gradient(to bottom, #fafafa, #f9fafb); border: 2px solid #e5e7eb; border-radius: 12px;\">" +
                "                    <div style=\"font-size: 48px; font-weight: 700; color: #000000; letter-spacing: 16px; font-family: 'Courier New', Consolas, monospace; line-height: 1.2;\">" + otp + "</div>" +
                "                  </td>" +
                "                </tr>" +
                "              </table>" +
                "              " +
                "              <!-- Expiry Warning -->" +
                "              <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse; margin-bottom: 24px;\">" +
                "                <tr>" +
                "                  <td style=\"padding: 20px 24px; background-color: #fafafa; border-left: 4px solid #000000; border-radius: 6px;\">" +
                "                    <p style=\"margin: 0; font-size: 14px; line-height: 1.6; color: #000000; font-weight: 600;\">⏰ Mã này sẽ hết hạn sau 15 phút</p>" +
                "                  </td>" +
                "                </tr>" +
                "              </table>" +
                "              " +
                "              <!-- Security Warning -->" +
                "              <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse; margin-bottom: 0;\">" +
                "                <tr>" +
                "                  <td style=\"padding: 20px 24px; background-color: #fef2f2; border-left: 4px solid #dc2626; border-radius: 6px;\">" +
                "                    <p style=\"margin: 0; font-size: 14px; line-height: 1.6; color: #991b1b;\"><strong style=\"font-weight: 600;\">⚠️ Lưu ý:</strong> Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này. Mật khẩu của bạn sẽ không thay đổi.</p>" +
                "                  </td>" +
                "                </tr>" +
                "              </table>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style=\"padding: 40px; text-align: center; border-top: 1px solid #e5e7eb; background-color: #fafafa;\">" +
                "              <p style=\"margin: 0 0 8px; font-size: 13px; line-height: 1.5; color: #a3a3a3;\">Đây là email tự động, vui lòng không trả lời email này.</p>" +
                "              <p style=\"margin: 0; font-size: 12px; color: #d4d4d4;\">© 2025 Chattrix. All rights reserved.</p>" +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }

    /**
     * Build HTML template for welcome email - Shadcn UI inspired (Black & White)
     */
    private String buildWelcomeEmailTemplate(String fullName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif; background-color: #fafafa;\">" +
                "  <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse;\">" +
                "    <tr>" +
                "      <td align=\"center\" style=\"padding: 40px 20px;\">" +
                "        <!-- Main Container -->" +
                "        <table role=\"presentation\" style=\"max-width: 600px; width: 100%; border-collapse: collapse; background-color: #ffffff; border: 1px solid #e5e7eb; border-radius: 12px; box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);\">" +
                "          <!-- Header -->" +
                "          <tr>" +
                "            <td style=\"padding: 48px 40px 40px; text-align: center; border-bottom: 1px solid #e5e7eb;\">" +
                "              <h1 style=\"margin: 0; font-size: 28px; font-weight: 700; color: #000000; letter-spacing: -0.025em;\">Chattrix</h1>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Content -->" +
                "          <tr>" +
                "            <td style=\"padding: 48px 40px;\">" +
                "              <!-- Welcome Icon -->" +
                "              <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse; margin-bottom: 32px;\">" +
                "                <tr>" +
                "                  <td align=\"center\">" +
                "                    <div style=\"display: inline-block; width: 80px; height: 80px; background: linear-gradient(135deg, #fafafa 0%, #f3f4f6 100%); border: 2px solid #e5e7eb; border-radius: 50%; line-height: 76px; font-size: 40px; text-align: center;\">👋</div>" +
                "                  </td>" +
                "                </tr>" +
                "              </table>" +
                "              " +
                "              <!-- Title -->" +
                "              <h2 style=\"margin: 0 0 16px; font-size: 24px; font-weight: 600; color: #000000; text-align: center; letter-spacing: -0.02em;\">Chào Mừng Đến Với Chattrix!</h2>" +
                "              " +
                "              <!-- Greeting -->" +
                "              <p style=\"margin: 0 0 32px; font-size: 16px; line-height: 1.6; color: #525252; text-align: center;\">Xin chào <strong style=\"color: #000000; font-weight: 600;\">" + fullName + "</strong>,</p>" +
                "              " +
                "              <!-- Message -->" +
                "              <p style=\"margin: 0 0 40px; font-size: 15px; line-height: 1.7; color: #525252; text-align: center;\">Chúc mừng bạn đã tạo tài khoản Chattrix thành công! Bạn đã sẵn sàng để bắt đầu trò chuyện với bạn bè và đồng nghiệp trong thời gian thực.</p>" +
                "              " +
                "              <!-- Features Box -->" +
                "              <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse; margin-bottom: 32px;\">" +
                "                <tr>" +
                "                  <td style=\"padding: 32px 28px; background: linear-gradient(to bottom, #fafafa, #f9fafb); border: 1px solid #e5e7eb; border-radius: 12px;\">" +
                "                    <!-- Feature 1 -->" +
                "                    <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse; margin-bottom: 20px;\">" +
                "                      <tr>" +
                "                        <td style=\"width: 40px; vertical-align: top;\">" +
                "                          <div style=\"width: 36px; height: 36px; background-color: #ffffff; border: 1px solid #e5e7eb; border-radius: 8px; text-align: center; line-height: 36px; font-size: 18px;\">✨</div>" +
                "                        </td>" +
                "                        <td style=\"vertical-align: top; padding-left: 16px;\">" +
                "                          <p style=\"margin: 0 0 4px; font-size: 15px; font-weight: 600; color: #000000;\">Nhắn tin real-time</p>" +
                "                          <p style=\"margin: 0; font-size: 14px; line-height: 1.5; color: #737373;\">Trò chuyện tức thì với bạn bè</p>" +
                "                        </td>" +
                "                      </tr>" +
                "                    </table>" +
                "                    <!-- Feature 2 -->" +
                "                    <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse; margin-bottom: 20px;\">" +
                "                      <tr>" +
                "                        <td style=\"width: 40px; vertical-align: top;\">" +
                "                          <div style=\"width: 36px; height: 36px; background-color: #ffffff; border: 1px solid #e5e7eb; border-radius: 8px; text-align: center; line-height: 36px; font-size: 18px;\">👥</div>" +
                "                        </td>" +
                "                        <td style=\"vertical-align: top; padding-left: 16px;\">" +
                "                          <p style=\"margin: 0 0 4px; font-size: 15px; font-weight: 600; color: #000000;\">Nhóm chat đa dạng</p>" +
                "                          <p style=\"margin: 0; font-size: 14px; line-height: 1.5; color: #737373;\">Tạo và quản lý nhóm dễ dàng</p>" +
                "                        </td>" +
                "                      </tr>" +
                "                    </table>" +
                "                    <!-- Feature 3 -->" +
                "                    <table role=\"presentation\" style=\"width: 100%; border-collapse: collapse;\">" +
                "                      <tr>" +
                "                        <td style=\"width: 40px; vertical-align: top;\">" +
                "                          <div style=\"width: 36px; height: 36px; background-color: #ffffff; border: 1px solid #e5e7eb; border-radius: 8px; text-align: center; line-height: 36px; font-size: 18px;\">🔒</div>" +
                "                        </td>" +
                "                        <td style=\"vertical-align: top; padding-left: 16px;\">" +
                "                          <p style=\"margin: 0 0 4px; font-size: 15px; font-weight: 600; color: #000000;\">Bảo mật tối đa</p>" +
                "                          <p style=\"margin: 0; font-size: 14px; line-height: 1.5; color: #737373;\">Dữ liệu được mã hóa an toàn</p>" +
                "                        </td>" +
                "                      </tr>" +
                "                    </table>" +
                "                  </td>" +
                "                </tr>" +
                "              </table>" +
                "              " +
                "              <!-- CTA Message -->" +
                "              <p style=\"margin: 0; font-size: 15px; line-height: 1.6; color: #525252; text-align: center; font-weight: 500;\">Chúc bạn có trải nghiệm tuyệt vời! 🎉</p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style=\"padding: 40px; text-align: center; border-top: 1px solid #e5e7eb; background-color: #fafafa;\">" +
                "              <p style=\"margin: 0 0 8px; font-size: 13px; line-height: 1.5; color: #a3a3a3;\">Đây là email tự động, vui lòng không trả lời email này.</p>" +
                "              <p style=\"margin: 0; font-size: 12px; color: #d4d4d4;\">© 2025 Chattrix. All rights reserved.</p>" +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }
}
