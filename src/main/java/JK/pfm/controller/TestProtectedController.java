
package jk.pfm.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestProtectedController {
    
    @GetMapping("/api/protected")
    public String checkProtected (){
    return "You can see this!";
}
}
