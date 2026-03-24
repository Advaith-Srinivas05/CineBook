package com.CineBook.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NoCacheFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse http = (HttpServletResponse) response;
            http.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
            http.setHeader("Pragma", "no-cache"); // HTTP 1.0
            http.setDateHeader("Expires", 0); // Proxies
        }
        chain.doFilter(request, response);
    }
}
