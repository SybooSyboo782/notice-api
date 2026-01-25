package syboo.notice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import syboo.notice.config.TestClockConfig;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@Transactional
@Import(TestClockConfig.class)
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {
    @Autowired
    protected MockMvc mockMvc;
}
