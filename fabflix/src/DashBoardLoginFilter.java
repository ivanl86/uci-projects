import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebFilter(filterName = "DashBoardLoginFilter", urlPatterns = {"/employee/*", "/_dashboard/*"})
public class DashBoardLoginFilter implements Filter {

    private final List<String> allowedURLs;

    public DashBoardLoginFilter() {

        allowedURLs = new ArrayList<>();
    }
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        allowedURLs.add("dashboard-login.html");
        allowedURLs.add("dashboard-login.js");
        allowedURLs.add("dashboard/login");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        // Always check if this URL is allowed to access without logging in
        if (this.isURLAllowedWithoutLogin(httpRequest.getRequestURI())) {
            // If this URL is allowed, return
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Redirect to login page if the "user" attribute doesn't exist in session
        if (httpRequest.getSession().getAttribute("employee") == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/dashboard-login.html");
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
    }

    private boolean isURLAllowedWithoutLogin(String requestURL) {
        // Setup your own rules here to allow accessing some resource without logging in
        return allowedURLs.stream().anyMatch(requestURL.toLowerCase()::endsWith);
    }
}
