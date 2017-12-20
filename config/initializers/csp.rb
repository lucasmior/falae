SecureHeaders::Configuration.default do |config|
  config.cookies = {
    secure: true,
    httponly: true,
    samesite: {
      lax: true
    }
  }

  # Add "; preload" and submit the site to hstspreload.org for best protection.
  config.hsts = "max-age=#{1.year.to_i}; includeSubdomains"
  config.x_frame_options = "DENY"
  config.x_content_type_options = "nosniff"
  config.x_xss_protection = "1; mode=block"
  config.x_download_options = "noopen"
  config.x_permitted_cross_domain_policies = "none"
  config.referrer_policy = "strict-origin-when-cross-origin"

  config.clear_site_data = [ "cookies", "storage" ]

  # CSP allows inline scripts/style due application requirements
  config.csp = {
    preserve_schemes: true,
    base_uri: %w('self'),
    default_src: %w('self'),
    child_src: %w('none'),
    media_src: %w('none'),
    object_src: %w('none'),
    img_src: %w('self' data:),
    script_src: %w('self' 'unsafe-inline'),
    style_src: %w('self' 'unsafe-inline'),
    block_all_mixed_content: true,
  }

  config.csp_report_only = config.csp.merge({
    report_uri: %w(/csp-report)
  })
end
