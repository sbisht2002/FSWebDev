package com.fswebdev.fit90.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PlanGeneratorTest {

    static JSONObject config() throws Exception {
        String dir = System.getProperty("content.dir", "../content");
        return new JSONObject(new String(Files.readAllBytes(Paths.get(dir, "en", "plan.json")), "UTF-8"));
    }

    /** Every lesson and audio id the config mentions, as if all content existed. */
    static Set<String> allArticles(JSONObject c) {
        Set<String> s = new HashSet<String>();
        s.add(c.optString("safety"));
        JSONArray f = c.optJSONArray("foundation");
        for (int i = 0; i < f.length(); i++) s.add(f.optString(i));
        JSONObject life = c.optJSONObject("lifestyle");
        for (Iterator<String> k = life.keys(); k.hasNext(); ) s.add(life.optString(k.next()));
        JSONObject t = c.optJSONObject("tracks");
        for (Iterator<String> k = t.keys(); k.hasNext(); ) {
            JSONArray a = t.optJSONArray(k.next());
            for (int i = 0; i < a.length(); i++) s.add(a.optString(i));
        }
        return s;
    }

    static Set<String> allAudio(JSONObject c) {
        Set<String> s = new HashSet<String>();
        JSONObject a = c.optJSONObject("audio");
        for (Iterator<String> k = a.keys(); k.hasNext(); ) {
            JSONArray pool = a.optJSONArray(k.next());
            for (int i = 0; i < pool.length(); i++) s.add(pool.optJSONObject(i).optString("id"));
        }
        return s;
    }

    static Profile profile(int minutes, String... goals) {
        Profile p = new Profile();
        p.minutes = minutes;
        for (String g : goals) p.goals.add(g);
        return p;
    }

    static String flatten(PlanGenerator.Plan plan) {
        StringBuilder sb = new StringBuilder();
        for (PlanGenerator.Week w : plan.weeks) for (List<PlanGenerator.Session> d : w.days) {
            for (PlanGenerator.Session s : d) sb.append(s.key()).append(s.minutes).append(s.optional).append(';');
            sb.append('|');
        }
        return sb.toString();
    }

    @Test
    public void deterministic() throws Exception {
        JSONObject c = config();
        Profile p = profile(15, Profile.LAST_LONGER, Profile.LESS_ANXIETY);
        assertEquals(flatten(PlanGenerator.generate(p, c, allArticles(c), allAudio(c))),
                flatten(PlanGenerator.generate(p, c, allArticles(c), allAudio(c))));
    }

    @Test
    public void eightWeeksOfOneToThreeSessionsWithinBudget() throws Exception {
        JSONObject c = config();
        for (int minutes : new int[]{5, 10, 15, 20}) {
            String[][] goalSets = {{Profile.LAST_LONGER}, {Profile.FIRMER}, {Profile.LESS_ANXIETY, Profile.INTIMACY},
                    {Profile.FITNESS}, {Profile.LAST_LONGER, Profile.FIRMER, Profile.LESS_ANXIETY, Profile.INTIMACY, Profile.FITNESS}};
            for (String[] goals : goalSets) {
                Profile p = profile(minutes, goals);
                p.partner = "partner";
                PlanGenerator.Plan plan = PlanGenerator.generate(p, c, allArticles(c), allAudio(c));
                assertEquals(8, plan.weeks.size());
                for (PlanGenerator.Week w : plan.weeks) {
                    assertEquals(7, w.days.size());
                    for (List<PlanGenerator.Session> day : w.days) {
                        int required = 0, used = 0;
                        for (PlanGenerator.Session s : day) {
                            if (!s.optional) {
                                required++;
                                used += s.minutes;
                            }
                        }
                        assertTrue("1-3 sessions", required >= 1 && required <= 3);
                        assertTrue("budget " + minutes + " used " + used, used <= minutes || required == 1);
                    }
                }
            }
        }
    }

    @Test
    public void lessonsOnlyReferenceAvailableContent() throws Exception {
        JSONObject c = config();
        Set<String> available = new HashSet<String>();
        available.add("pf-01");
        available.add("ll-01");
        PlanGenerator.Plan plan = PlanGenerator.generate(profile(20, Profile.LAST_LONGER), c, available, new HashSet<String>());
        for (PlanGenerator.Week w : plan.weeks) for (List<PlanGenerator.Session> d : w.days) for (PlanGenerator.Session s : d) {
            if ("lesson".equals(s.type)) assertTrue(available.contains(s.ref));
            assertFalse("no audio when none exists", "audio".equals(s.type));
        }
    }

    @Test
    public void relaxationFirstForPainAndAnxiety() throws Exception {
        JSONObject c = config();
        Profile pain = profile(10, Profile.LAST_LONGER);
        pain.health.add(Profile.PELVIC_PAIN);
        PlanGenerator.Plan plan = PlanGenerator.generate(pain, c, allArticles(c), allAudio(c));
        assertTrue(plan.relaxationFirst);
        for (PlanGenerator.Week w : plan.weeks) for (List<PlanGenerator.Session> d : w.days) for (PlanGenerator.Session s : d) {
            if ("kegel".equals(s.type)) assertEquals("pain keeps relaxation variant all 8 weeks", "relax", s.ref);
        }
        Profile anxious = profile(10, Profile.LESS_ANXIETY);
        anxious.health.add(Profile.ANXIETY_LOW_MOOD);
        PlanGenerator.Plan p2 = PlanGenerator.generate(anxious, c, allArticles(c), allAudio(c));
        assertEquals("relax", kegelRef(p2, 0));
        assertEquals("standard", kegelRef(p2, 3));
    }

    static String kegelRef(PlanGenerator.Plan plan, int week) {
        for (PlanGenerator.Session s : plan.weeks.get(week).days.get(0)) if ("kegel".equals(s.type)) return s.ref;
        return null;
    }

    @Test
    public void sensateFocusOnlyWithAPartner() throws Exception {
        JSONObject c = config();
        Profile single = profile(20, Profile.INTIMACY);
        single.partner = "single";
        Profile partnered = profile(20, Profile.INTIMACY);
        partnered.partner = "partner";
        assertFalse(flatten(PlanGenerator.generate(single, c, allArticles(c), allAudio(c))).contains("sensate"));
        assertTrue(flatten(PlanGenerator.generate(partnered, c, allArticles(c), allAudio(c))).contains("sensate"));
    }

    @Test
    public void smokingLessonComesInTheFirstTwoWeeks() throws Exception {
        JSONObject c = config();
        Profile p = profile(10, Profile.FIRMER);
        p.smoking = "daily";
        PlanGenerator.Plan plan = PlanGenerator.generate(p, c, allArticles(c), allAudio(c));
        boolean found = false;
        for (int w = 0; w < 2; w++) for (List<PlanGenerator.Session> d : plan.weeks.get(w).days) {
            for (PlanGenerator.Session s : d) if ("ls-03".equals(s.ref)) found = true;
        }
        assertTrue(found);
    }

    @Test
    public void walksAreOptionalAndOnlyForErectionOrFitnessGoals() throws Exception {
        JSONObject c = config();
        Profile p = profile(10, Profile.LESS_ANXIETY);
        p.activity = "active";
        assertFalse(flatten(PlanGenerator.generate(p, c, allArticles(c), allAudio(c))).contains("walk:"));
        Profile f = profile(10, Profile.FIRMER);
        f.activity = "active";
        PlanGenerator.Plan plan = PlanGenerator.generate(f, c, allArticles(c), allAudio(c));
        List<PlanGenerator.Session> walks = new ArrayList<PlanGenerator.Session>();
        for (PlanGenerator.Week w : plan.weeks) for (List<PlanGenerator.Session> d : w.days)
            for (PlanGenerator.Session s : d) if ("walk".equals(s.type)) walks.add(s);
        assertEquals(24, walks.size());
        for (PlanGenerator.Session s : walks) assertTrue(s.optional);
        assertEquals(40, walks.get(walks.size() - 1).minutes);
    }
}
