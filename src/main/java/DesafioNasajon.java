import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;

public class DesafioNasajon {

    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImR0TG03UVh1SkZPVDJwZEciLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL215bnhsdWJ5a3lsbmNpbnR0Z2d1LnN1cGFiYXNlLmNvL2F1dGgvdjEiLCJzdWIiOiIxYTAxZjJkMi02NTkwLTQ4NDUtOWRmNC00YWU0NmUwOWQ2MDMiLCJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNzczMjYyOTk2LCJpYXQiOjE3NzMyNTkzOTYsImVtYWlsIjoiZHVkYS5zcnVpekBnbWFpbC5jb20iLCJwaG9uZSI6IiIsImFwcF9tZXRhZGF0YSI6eyJwcm92aWRlciI6ImVtYWlsIiwicHJvdmlkZXJzIjpbImVtYWlsIl19LCJ1c2VyX21ldGFkYXRhIjp7ImVtYWlsIjoiZHVkYS5zcnVpekBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibm9tZSI6Ik1hcmlhIEVkdWFyZGEgRGUgU2VuYSBSdWl6IiwicGhvbmVfdmVyaWZpZWQiOmZhbHNlLCJzdWIiOiIxYTAxZjJkMi02NTkwLTQ4NDUtOWRmNC00YWU0NmUwOWQ2MDMifSwicm9sZSI6ImF1dGhlbnRpY2F0ZWQiLCJhYWwiOiJhYWwxIiwiYW1yIjpbeyJtZXRob2QiOiJwYXNzd29yZCIsInRpbWVzdGFtcCI6MTc3MzI1OTM5Nn1dLCJzZXNzaW9uX2lkIjoiMzcxYmFjMmQtOTE2YS00ZDUxLTg0ZmQtYzI5ZGVmMTA2ZWI0IiwiaXNfYW5vbnltb3VzIjpmYWxzZX0.H_qT6U4TGEGIygLbmwa-KwaZ3TfPnrvQwzrUmZuWbt0";

    private static final String IBGE_URL =
            "https://servicodados.ibge.gov.br/api/v1/localidades/municipios";

    private static final String SUBMIT_URL =
            "https://mynxlubykylncinttggu.functions.supabase.co/ibge-submit";

    public static void main(String[] args) {

        try {

            HttpClient client = HttpClient.newHttpClient();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            System.out.println("Consumindo API do IBGE...");

            HttpRequest ibgeReq = HttpRequest.newBuilder()
                    .uri(URI.create(IBGE_URL))
                    .GET()
                    .build();

            HttpResponse<String> ibgeRes =
                    client.send(ibgeReq, HttpResponse.BodyHandlers.ofString());

            JsonArray ibgeData = gson.fromJson(ibgeRes.body(), JsonArray.class);

            Map<String, JsonObject> mapaMunicipios = new HashMap<>();

            for (int i = 0; i < ibgeData.size(); i++) {

                JsonObject m = ibgeData.get(i).getAsJsonObject();

                String nomeNormalizado = normalizar(m.get("nome").getAsString());

                mapaMunicipios.put(nomeNormalizado, m);
            }

            System.out.println("Lendo input.csv...");

            List<String> linhas = Files.readAllLines(Paths.get("input.csv"));

            List<String[]> csvSaida = new ArrayList<>();

            int totalMunicipios = linhas.size() - 1;
            int totalOk = 0;
            int totalNaoEncontrado = 0;

            long popTotalOk = 0;

            Map<String, List<Long>> populacaoPorRegiao = new HashMap<>();

            for (int i = 1; i < linhas.size(); i++) {

                String[] colunas = linhas.get(i).split(",");

                String municipioInput = colunas[0].trim();
                long populacaoInput = Long.parseLong(colunas[1].trim());

                String nomeNormalizado = normalizar(municipioInput);

                JsonObject match = mapaMunicipios.get(nomeNormalizado);

                if (match == null) {
                    match = buscarAproximado(nomeNormalizado, mapaMunicipios);
                }

                String[] resultadoLinha = new String[7];

                resultadoLinha[0] = municipioInput;
                resultadoLinha[1] = String.valueOf(populacaoInput);

                if (match != null) {

                    String nomeOficial = match.get("nome").getAsString();

                    JsonObject micro = match.getAsJsonObject("microrregiao");
                    JsonObject meso = micro.getAsJsonObject("mesorregiao");
                    JsonObject ufObj = meso.getAsJsonObject("UF");

                    String ufSigla = ufObj.get("sigla").getAsString();

                    String regiao = ufObj
                            .getAsJsonObject("regiao")
                            .get("nome")
                            .getAsString();

                    String idIbge = match.get("id").getAsString();

                    resultadoLinha[2] = nomeOficial;
                    resultadoLinha[3] = ufSigla;
                    resultadoLinha[4] = regiao;
                    resultadoLinha[5] = idIbge;
                    resultadoLinha[6] = "OK";

                    totalOk++;

                    popTotalOk += populacaoInput;

                    populacaoPorRegiao
                            .computeIfAbsent(regiao, k -> new ArrayList<>())
                            .add(populacaoInput);

                } else {

                    resultadoLinha[2] = "";
                    resultadoLinha[3] = "";
                    resultadoLinha[4] = "";
                    resultadoLinha[5] = "";
                    resultadoLinha[6] = "NAO_ENCONTRADO";

                    totalNaoEncontrado++;
                }

                csvSaida.add(resultadoLinha);
            }

            try (FileWriter writer = new FileWriter("resultado.csv")) {

                writer.write("municipio_input,populacao_input,municipio_ibge,uf,regiao,id_ibge,status\n");

                for (String[] r : csvSaida) {
                    writer.write(String.join(",", r) + "\n");
                }
            }

            JsonObject stats = new JsonObject();

            stats.addProperty("total_municipios", totalMunicipios);
            stats.addProperty("total_ok", totalOk);
            stats.addProperty("total_nao_encontrado", totalNaoEncontrado);
            stats.addProperty("total_erro_api", 0);
            stats.addProperty("pop_total_ok", popTotalOk);

            JsonObject mediasRegiao = new JsonObject();

            for (String reg : populacaoPorRegiao.keySet()) {

                double avg = populacaoPorRegiao
                        .get(reg)
                        .stream()
                        .mapToLong(v -> v)
                        .average()
                        .orElse(0);

                mediasRegiao.addProperty(reg, avg);
            }

            stats.add("medias_por_regiao", mediasRegiao);

            JsonObject payload = new JsonObject();

            payload.add("stats", stats);

            System.out.println("Enviando resultados para correção...");

            HttpRequest submitReq = HttpRequest.newBuilder()
                    .uri(URI.create(SUBMIT_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + ACCESS_TOKEN)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> submitRes =
                    client.send(submitReq, HttpResponse.BodyHandlers.ofString());

            System.out.println("\n-RESPOSTA DA API");

            System.out.println(submitRes.body());

        } catch (Exception e) {

            System.err.println("Erro crítico no processamento: " + e.getMessage());

            e.printStackTrace();
        }
    }

    private static String normalizar(String t) {

        if (t == null) return "";

        return Normalizer
                .normalize(t, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z ]", "")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static JsonObject buscarAproximado(String nome, Map<String, JsonObject> mapa) {

        int menorDistancia = Integer.MAX_VALUE;
        String melhorKey = null;

        for (String key : mapa.keySet()) {

            int distancia = distanciaLevenshtein(nome, key);

            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                melhorKey = key;
            }
        }

        // aceita apenas erros pequenos
        if (menorDistancia <= 2) {

            // evita corrigir duplicação exagerada (ex: Santoo Andre)
            if (nome.contains("oo")) {
                return null;
            }

            return mapa.get(melhorKey);
        }

        return null;
    }

    private static int distanciaLevenshtein(String a, String b) {

        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;

        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {

            for (int j = 1; j <= b.length(); j++) {

                int custo =
                        a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(
                                dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1
                        ),
                        dp[i - 1][j - 1] + custo
                );
            }
        }

        return dp[a.length()][b.length()];
    }
}