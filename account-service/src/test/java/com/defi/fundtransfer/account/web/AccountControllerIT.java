package com.defi.fundtransfer.account.web;

import com.defi.fundtransfer.account.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AccountControllerIT extends PostgresIntegrationTest {

    @Autowired
    MockMvc mvc;

    private String openAccount(long balance) throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"Defi","currency":"INR","initialBalanceMinor":%d}
                                """.formatted(balance)))
                .andExpect(status().isCreated())
                .andReturn();
        Matcher m = Pattern.compile("\"id\":\"([0-9a-f-]{36})\"").matcher(res.getResponse().getContentAsString());
        if (!m.find()) {
            throw new AssertionError("no id in response");
        }
        return m.group(1);
    }

    private String debitBody(UUID transferId, long amount) {
        return "{\"transferId\":\"%s\",\"amountMinor\":%d}".formatted(transferId, amount);
    }

    @Test
    void debitReturns201ThenReplayReturns200() throws Exception {
        String id = openAccount(10_000);
        UUID transferId = UUID.randomUUID();

        mvc.perform(post("/api/v1/accounts/{id}/debit", id).contentType(MediaType.APPLICATION_JSON).content(debitBody(transferId, 1_000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfterMinor").value(9_000))
                .andExpect(jsonPath("$.replayed").value(false));

        mvc.perform(post("/api/v1/accounts/{id}/debit", id).contentType(MediaType.APPLICATION_JSON).content(debitBody(transferId, 1_000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true));
    }

    @Test
    void insufficientFundsMapsTo422() throws Exception {
        String id = openAccount(500);

        mvc.perform(post("/api/v1/accounts/{id}/debit", id).contentType(MediaType.APPLICATION_JSON).content(debitBody(UUID.randomUUID(), 501)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Insufficient funds"));
    }

    @Test
    void unknownAccountMapsTo404() throws Exception {
        mvc.perform(get("/api/v1/accounts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestMapsTo400() throws Exception {
        mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"\",\"currency\":\"rupees\",\"initialBalanceMinor\":-5}"))
                .andExpect(status().isBadRequest());
    }
}
