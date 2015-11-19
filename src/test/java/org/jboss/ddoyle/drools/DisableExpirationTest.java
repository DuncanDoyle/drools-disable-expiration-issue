package org.jboss.ddoyle.drools;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.drools.compiler.CommonTestMethodBase;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.core.time.impl.PseudoClockScheduler;
import org.jboss.ddoyle.drools.model.SimpleEvent;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.ReleaseId;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;


/**
 * Tests disabling expiration.
 * 
 * @author <a href="mailto:duncan.doyle@redhat.com">Duncan Doyle</a>
 */
public class DisableExpirationTest extends CommonTestMethodBase {

	/**
	 * Tests disabling expiration.
	 */
	@Test
	public void testEventExpiration() {
		//@formatter:off
		String kmoduleContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
								"<kmodule xmlns=\"http://jboss.org/kie/6.0.0/kmodule\">\n" +
									"<kbase name=\"rules\" equalsBehavior=\"equality\" eventProcessingMode=\"stream\" default=\"true\">\n" +
										"<ksession name=\"ksession-rules\" default=\"true\" type=\"stateful\" clockType=\"pseudo\"/>\n" +
										"</kbase>\n" + 
								"</kmodule>";
		
		String drl1 = "package org.jboss.ddoyle.drools.rules\n" +
					"\n" +
					"import org.jboss.ddoyle.drools.model.SimpleEvent\n" +
					"\n" +
					"declare SimpleEvent\n" +
						"@role( event )\n" +
					    "@timestamp( timestamp )\n" +
						"@expires( -1 )\n" +
					 "end\n" +
					 "\n" +
					 "rule \"SimpleEventRule\"\n" +
					 "when\n" +
					 	"$s: SimpleEvent ()\n" +
					    "not SimpleEvent (this != $s, this after[0, 30s] $s)\n" +
					 "then\n" +
					     "System.out.println(\"No event within 30 seconds of this event: \" + $s.getId());\n" +
					 "end";
		//@formatter:on

		KieServices ks = KieServices.Factory.get();

		ReleaseId releaseId1 = ks.newReleaseId("org.kie", "test-event-expiration", "1.0.0");
		KieModule km = createAndDeployJar(ks, kmoduleContent, releaseId1, drl1);

		KieContainer kc = ks.newKieContainer(km.getReleaseId());
		KieSession ksession = kc.newKieSession();

		PseudoClockScheduler clock = ksession.getSessionClock();
		
		SimpleEvent event1 = new SimpleEvent("1", 0);
		ksession.insert(event1);
		ksession.fireAllRules();
		
		//Session should only contain the fact we just inserted.
		assertEquals(1, ksession.getFactCount());
		Collection<FactHandle> factHandles = ksession.getFactHandles();
		
		clock.advanceTime(60000, TimeUnit.MILLISECONDS);
		ksession.fireAllRules();

		//We've disabled expiration, so fact should still be in WorkingMemory.
		assertEquals(1, ksession.getFactCount());
		
		ksession.dispose();
	}

	

	/*
	 * @Mario: I added the following method in the test class, because CommenTestMethodBase does not provided a 'createAndDeployJar' that
	 * accepts both String DRLs and kModuleContent. You might want to add these 'createAndDeployJar' and 'createJar' to the
	 * CommonTestMethodBase class.
	 */
	public static KieModule createAndDeployJar(KieServices ks, String kmoduleContent, ReleaseId releaseId, String... drls) {
		byte[] jar = createJar(ks, kmoduleContent, releaseId, drls);

		// Deploy jar into the repository
		KieModule km = deployJarIntoRepository(ks, jar);
		return km;
	}

	public static byte[] createJar(KieServices ks, String kmoduleContent, ReleaseId releaseId, String... drls) {
		KieFileSystem kfs = ks.newKieFileSystem().generateAndWritePomXML(releaseId).writeKModuleXML(kmoduleContent);
		for (int i = 0; i < drls.length; i++) {
			if (drls[i] != null) {
				kfs.write("src/main/resources/r" + i + ".drl", drls[i]);
			}
		}
		KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
		assertFalse(kb.getResults().getMessages(org.kie.api.builder.Message.Level.ERROR).toString(),
				kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR));
		InternalKieModule kieModule = (InternalKieModule) ks.getRepository().getKieModule(releaseId);
		byte[] jar = kieModule.getBytes();
		return jar;
	}

	private static KieModule deployJarIntoRepository(KieServices ks, byte[] jar) {
		Resource jarRes = ks.getResources().newByteArrayResource(jar);
		KieModule km = ks.getRepository().addKieModule(jarRes);
		return km;
	}

}
