package ru.joker

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.netflix.feign.EnableFeignClients
import org.springframework.cloud.netflix.hystrix.EnableHystrix
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import java.util.concurrent.atomic.AtomicInteger

import static com.netflix.hystrix.HystrixCommandGroupKey.Factory.*

@Slf4j
@EnableFeignClients
@EnableDiscoveryClient
@EnableHystrix
@SpringBootApplication
public class HippoApplication {
  public static final int DEFAULT_PADDING = 50

  public static void main(String[] args) {
    println 'Starting'.center(DEFAULT_PADDING, '=')
    SpringApplication.run HippoApplication, args
    println 'Started'.center(DEFAULT_PADDING, '=')
  }

  @RestController
  public static class HippoController {
    private AtomicInteger hippoCount = new AtomicInteger(Integer.MAX_VALUE);

    @Autowired
    FbiClient fbiClient
    @Autowired
    ParrotClient parrotClient

    FbiClient client

    @RequestMapping(value = '/rent', method = RequestMethod.GET)
    def rent(@RequestParam Optional<Integer> count) {
      log.warn('hippo!!!')

      def hippoRequest = count.orElse(1)

      def fee = 0
      try {
        ParrotClient.ParrotResponse feeResponse = parrotClient.getFee()
        fee = feeResponse.parrotFee
      } finally {
        new FbiCommand(asKey("fbi-service"), client, hippoRequest, fee).observe()
            .doOnNext { println "next $it" }
            .doOnError { println "error $it" }
      }

      log.info('hippo end!!!')

      return [hippoRemain: hippoCount.getAndAdd(-1 * hippoRequest),
              parrot_fee : fee]
    }
  }
}

@Slf4j
class FbiCommand extends HystrixCommand<FbiClient.FinkResponse> {
  private final FbiClient client
  private final int fee
  private final int hippoCount

  protected FbiCommand(HystrixCommandGroupKey group,
                       FbiClient client,
                       int hippoCount, int fee) {
    super(group)
    this.hippoCount = hippoCount
    this.fee = fee
    this.client = client
  }

  @Override
  protected FbiClient.FinkResponse run() throws Exception {
    return client.fink(new FbiClient.Fink(hippoCount, fee))
  }

  @Override
  protected FbiClient.FinkResponse getFallback() {
    log.info 'fallback'
    return new FbiClient.FinkResponse();
  }
}