@import de.morihofi.acmeserver.certificate.api.serverInfo.objects.ServerInfoResponse
@import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager
@import de.morihofi.acmeserver.webui.JteLocalizer
@import de.morihofi.acmeserver.webui.handler.StatsHandler
@import io.javalin.http.Context
@import java.util.List

@param ServerInfoResponse serverInfoResponse
@param CryptoStoreManager cryptoStoreManager
@param JteLocalizer localizer
@param Context context
@param List<StatsHandler.StatisticItem> statisticsAll
@param List<StatsHandler.ProvisionerStatistic> statisticsProvisioner
@param List<StatsHandler.StatisticItem> statisticsDatabase


<!DOCTYPE html>
<html>
<head>
    @template.layout.head()

    <title>${localizer.lookup("app.name")}</title>
    <style>
        .chart-container {
          position: relative;
          margin: auto;
          height: 80vh;
          width: 100%;
        }
    </style>
</head>
<body>
@template.layout.navigation(
localizer = localizer,
context = context
)

<main class="container">
    <h1>${localizer.lookup("web.stats.title")}</h1>
    <p>${localizer.lookup("web.stats.intro")}</p>

    <div class="container mt-3 mb-3">
        <div class="row">
            @for(StatsHandler.StatisticItem item : statisticsAll)
                <div class="col-md-3 mb-3">
                    <div class="card text-center">
                        <div class="card-body">
                            <h5 class="card-title display-6 fw-bold">${item.number()}</h5>
                            <p class="card-text">${localizer.lookup(item.translationKey())}</p>
                        </div>
                    </div>
                </div>
            @endfor
        </div>
    </div>


    <h2>${localizer.lookup("web.stats.perProvisioner.title")}</h2>
    <p>${localizer.lookup("web.stats.perProvisioner.intro")}</p>

    @for(StatsHandler.ProvisionerStatistic statistic: statisticsProvisioner)
        <div class="card mb-3">
            <a href="/provisioner-info?name=${statistic.provisionerName()}" class="position-absolute top-0 end-0 p-2 text-reset">
                <i class="fas fa-info-circle"></i>
            </a>
            <div class="card-header">
                ${statistic.provisionerName()}
            </div>
            <div class="card-body">
                <div class="container mt-3 mb-3">
                    <div class="row">
                        @for(StatsHandler.StatisticItem item : statistic.stats())
                            <div class="col-md-3 mb-3">
                                <div class="card text-center">
                                    <div class="card-body">
                                        <h5 class="card-title display-6 fw-bold">${item.number()}</h5>
                                        <p class="card-text">${localizer.lookup(item.translationKey())}</p>
                                    </div>
                                </div>
                            </div>
                        @endfor
                    </div>
                </div>
            </div>
            <div class="chart-container">
                <canvas class="chart-issuedCertificate mb-3 ml-3 mr-3" data-provisioner="${statistic.provisionerName()}"></canvas>
            </div>

        </div>

    @endfor


    <h2>${localizer.lookup("web.stats.database.title")}</h2>
    <p>${localizer.lookup("web.stats.database.intro")}</p>

    <div class="container mt-3 mb-3">
        <div class="row">
            @for(StatsHandler.StatisticItem item : statisticsDatabase)
                <div class="col-md-3 mb-3">
                    <div class="card text-center mb-3">
                        <div class="card-body">
                            <h5 class="card-title display-6 fw-bold">${item.number()}</h5>
                            <p class="card-text">${localizer.lookup(item.translationKey())}</p>
                        </div>
                    </div>
                </div>
            @endfor
        </div>
    </div>



</main>
@template.layout.footer(
serverInfoResponse = serverInfoResponse,
localizer = localizer
)

<script>

    function transformData(jsonData) {
    return Object.entries(jsonData).map(([date, count]) => ({
        x: new Date(date),
        y: count
    }));
    }


    // Get all charts
    const chartElements = document.getElementsByClassName("chart-issuedCertificate");
    
    // Iterate over every chart
    Array.from(chartElements).forEach((elem) => {
      const provisionerName = elem.getAttribute('data-provisioner');
      const ctx = elem.getContext('2d');

      // Initialize Chart.js chart instance
      const chart = new Chart(ctx, {
        type: 'line',
        data: {
          datasets: [{
            label: `@raw${provisionerName}@endraw`,
            backgroundColor: 'rgba(0, 123, 255, 0.5)',
            borderColor: 'rgba(0, 123, 255, 1)',
            fill: false,
            data: [] // Initially empty, will be filled later
          }]
        },
        options: {
          plugins: {
            title: {
              text: '${localizer.lookup("web.stats.database.chart.issuedCertificatesPerDay")}',
              display: true
            }
          },
          scales: {
            x: {
              type: 'time',
              time: {
                parser: 'yyyy-MM-dd',
                unit: 'day',
                displayFormats: {
                  day: 'yyyy-MM-dd'
                }
              },
              title: {
                display: true,
                text: 'Datum'
              }
            },
            y: {
              beginAtZero: true,
              title: {
                display: true,
                text: '${localizer.lookup("web.stats.database.chart.countOfIssuedCertificates")}'
              }
            }
          },
          responsive: true,
          maintainAspectRatio: false
        }
    });
@raw
    // Execute a GET request for the specific provisioner
    fetch(`/api/stats/provisioners/certificates-issued?provisioner=${encodeURIComponent(provisionerName)}`)
      .then(response => response.json())
      .then(jsonData => {
        // Transform the JSON object into a Chart.js-friendly format
        chart.data.datasets[0].data = transformData(jsonData);
        chart.update();
      })
      .catch(error => console.error(`Error when retrieving data for ${provisionerName}:`, error));
    });
@endraw

</script>


</body>
</html>