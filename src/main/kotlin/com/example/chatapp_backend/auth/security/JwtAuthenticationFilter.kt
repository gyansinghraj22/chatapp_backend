package com.example.chatapp_backend.auth.security

import com.example.chatapp_backend.persistence.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val users: UserRepository
) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val authorization = request.getHeader("Authorization")
        if (authorization.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }
        if (!authorization.startsWith("Bearer ") || authorization.removePrefix("Bearer ").isBlank()) {
            unauthorized(response)
            return
        }
        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }
        val userId = try {
            jwtService.parseSubject(authorization.removePrefix("Bearer ").trim())
        } catch (_: Exception) {
            unauthorized(response)
            return
        }
        val user = users.findById(userId).orElse(null)
        if (user == null || !user.isActive) {
            unauthorized(response)
            return
        }
        val auth = UsernamePasswordAuthenticationToken(
            AuthenticatedUser(user.id, user.username), null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth
        try {
            filterChain.doFilter(request, response)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    private fun unauthorized(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write("{\"error\":\"invalid_or_expired_access_token\"}")
    }
}
