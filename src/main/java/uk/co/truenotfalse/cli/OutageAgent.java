package uk.co.truenotfalse.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.truenotfalse.Version;
import uk.co.truenotfalse.agent.OutageAgentService;
import uk.co.truenotfalse.dao.impl.InterviewTestsMockApiDaoImpl;


public class OutageAgent
{
    public static final int SUCCESS_STATUS = 0;
    public static final int FAILURE_STATUS = 1;

    public static void main(final String... args) throws Exception
    {
        final Args parsedArgs = handleArgs(args);

        final OutageAgentService agent =
          new OutageAgentService(new InterviewTestsMockApiDaoImpl(parsedArgs.getApiKey(), parsedArgs.getBaseUri(),
                                                                  WebClient.create(Vertx.vertx())));

        agent.updateOutages(parsedArgs.getSiteId(), parsedArgs.getCutoff()).
          blockingSubscribe(() ->
          {
              LOG.info("Updated {}.", parsedArgs.getSiteId());
              System.out.println("Site outages updated.");
              Runtime.getRuntime().exit(SUCCESS_STATUS);
          },
          error ->
          {
              LOG.error("An error occurred.", error);
              System.err.println("Error: " + error.getMessage());
              Runtime.getRuntime().exit(FAILURE_STATUS);
          });
    }


    private static Args handleArgs(final String... args)
    {
        final Args parsedArgs = new Args();
        final JCommander commandLineParser = JCommander.newBuilder().addObject(parsedArgs).build();

        try
        {
            commandLineParser.parse(args);
        }
        catch(final ParameterException pe)
        {
            System.err.println(pe.getLocalizedMessage());
            commandLineParser.usage();
            Runtime.getRuntime().exit(FAILURE_STATUS);
        }

        if(parsedArgs.isHelp())
        {
            commandLineParser.usage();
            Runtime.getRuntime().exit(SUCCESS_STATUS);
        }
        else if(parsedArgs.isVersion())
        {
            printVersion();
            Runtime.getRuntime().exit(SUCCESS_STATUS);
        }

        return parsedArgs;
    }

    private static void printVersion()
    {
        final String version = APP_VERSION;

        System.out.println(version);
        LOG.info(version);
    }


    @Parameters(resourceBundle = "uk.co.truenotfalse.cli.Cli")
    static class Args
    {
        boolean isHelp()
        {
            return help;
        }

        public boolean isVersion()
        {
            return version;
        }

        public String getBaseUri()
        {
            return baseUri;
        }

        public String getSiteId()
        {
            return siteId;
        }

        public String getApiKey()
        {
            return apiKey;
        }

        public OffsetDateTime getCutoff()
        {
            return cutoff;
        }


        @Parameter(names = {ENDPOINT_BASE_OPTION, SHORT_ENDPOINT_BASE_OPTION}, validateWith=ArgsValidator.class,
                   description = "The base URI of the API instance to use.",
                   descriptionKey = "baseUri.description")
        private String baseUri = DEFAULT_BASE_URI;

        @Parameter(names = {SITE_ID_OPTION, SHORT_SITE_ID_OPTION}, validateWith=ArgsValidator.class,
                   description = "The ID of the site to query and update.",
                   descriptionKey = "siteId.description")
        private String siteId = DEFAULT_SITE_ID;

        @Parameter(names = {API_KEY_OPTION, SHORT_API_KEY_OPTION}, validateWith=ArgsValidator.class,
          description = "The key to use to authorize requests with the API.  This is a required parameter.",
          descriptionKey = "apiKey.description", required = true)
        private String apiKey;

        @Parameter(names = {CUTOFF_OPTION, SHORT_CUTOFF_OPTION}, validateWith=ArgsValidator.class,
                   converter = OffsetDatetimeConverter.class,
                   description = "The cutoff to apply the beginning timestamp of outage records.  Records with periods that begin prior to the cutoff are excluded.",
                   descriptionKey = "cutoff.description")
        private OffsetDateTime cutoff = DEFAULT_CUTOFF;

        @Parameter(names = {"--version"}, help = true, hidden = true,  description = "Displays version information and then exits.",
          descriptionKey = "versionOption.description")
        private boolean version = false;

        @Parameter(names = {"--help", "--?", "-?"}, help = true, description = "Displays this help and exits.",
                   descriptionKey = "helpOption.description")
        private boolean help = false;
    }


    public static class ArgsValidator implements IParameterValidator
    {
        @Override
        public void validate(final String name, final String value) throws ParameterException
        {
            switch(name)
            {
                case SITE_ID_OPTION, SHORT_SITE_ID_OPTION ->
                {
                    if(value.isBlank()) throw new ParameterException("Site ID is not valid.");
                }
                case API_KEY_OPTION, SHORT_API_KEY_OPTION ->
                {
                    if(value.isBlank()) throw new ParameterException("The API key is not valid.");
                }
                case ENDPOINT_BASE_OPTION, SHORT_ENDPOINT_BASE_OPTION ->
                {
                    try
                    {
                        new URL(value);
                    }
                    catch(final MalformedURLException mue)
                    {
                        throw new ParameterException("Base URI is not valid.");
                    }
                }
                case CUTOFF_OPTION, SHORT_CUTOFF_OPTION ->
                {
                    try
                    {
                        OffsetDateTime.parse(value, ISO_OFFSET_DATE_TIME);
                    }
                    catch(final DateTimeParseException dtpe)
                    {
                        throw new ParameterException("The cutoff format is not valid.");
                    }
                }
            }
        }
    }

    static class OffsetDatetimeConverter implements IStringConverter<OffsetDateTime>
    {
        @Override
        public OffsetDateTime convert(final String value)
        {
            return OffsetDateTime.parse(value, ISO_OFFSET_DATE_TIME);
        }
    }


    private static final String SITE_ID_OPTION = "--siteId";
    private static final String SHORT_SITE_ID_OPTION = "-s";
    private static final String ENDPOINT_BASE_OPTION = "--baseUri";
    private static final String SHORT_ENDPOINT_BASE_OPTION = "-b";
    private static final String API_KEY_OPTION = "--apiKey";
    private static final String SHORT_API_KEY_OPTION = "-a";
    private static final String CUTOFF_OPTION = "--cutoff";
    private static final String SHORT_CUTOFF_OPTION = "-c";

    private static final String DEFAULT_BASE_URI = "https://api.krakenflex.systems/interview-tests-mock-api/v1";
    private static final String DEFAULT_SITE_ID = "norwich-pear-tree";
    private static final OffsetDateTime DEFAULT_CUTOFF =
      OffsetDateTime.parse("2022-01-01T00:00:00.000Z", ISO_OFFSET_DATE_TIME);

    private static final Logger LOG = LoggerFactory.getLogger(OutageAgent.class);
    private static final String APP_VERSION = "Outage Agent/" + new Version().getVersion();
}
