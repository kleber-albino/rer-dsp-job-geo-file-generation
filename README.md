# RER DSP — Job Geo File Generation

**Projeto**: Rural Environmental Registry — Data Sharing Platform  
**Componente**: Job de Geração de Arquivos Geoespaciais  
**Tipo**: Digital Public Good (DPG)  
**Licença**: GPL-3.0

---

## 📋 Visão Geral

Job de geração de arquivos geoespaciais da plataforma DSP do RER. Responsável pela geração de shapefiles, GeoJSON e outros formatos geoespaciais a partir dos dados cadastrais ambientais rurais.

## 🏗️ Arquitetura

Este componente faz parte do ecossistema RER DSP:

```
rer-dsp-frontend (UI)
    ↓
rer-dsp-backend (API REST)
    ↓
rer-dsp-core (lógica de domínio)
    ↓
rer-dsp-job-data-migration (ETL)
rer-dsp-job-geo-file-generation  ← ESTE REPO
```

## 🚀 Setup

```bash
# Clonar
git clone https://github.com/Rural-Environmental-Registry/rer-dsp-job-geo-file-generation.git
cd rer-dsp-job-geo-file-generation

# Instruções de build serão adicionadas conforme desenvolvimento
```

## 📖 Documentação

- [RER — Visão Geral](https://github.com/Rural-Environmental-Registry)
- [SDD (System Design Document)](https://github.com/Rural-Environmental-Registry/core)

## 📜 Licença

Este projeto é licenciado sob a [GNU General Public License v3.0](LICENSE).
