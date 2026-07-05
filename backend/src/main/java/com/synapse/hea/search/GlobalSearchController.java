package com.synapse.hea.search;

import com.synapse.hea.common.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
public class GlobalSearchController {
  private final GlobalSearchService service;
  private final CurrentUser current;

  public GlobalSearchController(GlobalSearchService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  @GetMapping
  public GlobalSearchService.Result search(Authentication a, @RequestParam String q) {
    return service.search(current.id(a), q);
  }
}
