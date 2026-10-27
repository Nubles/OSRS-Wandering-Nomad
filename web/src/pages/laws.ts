/** The rules as the site states them. Each law matches the sim (NomadSim.java and sim.ts). */
export const LAWS = [
  {
    name: 'Inside',
    short: 'Everything you do happens inside your circle: skilling, combat, quests, shops, banking. No exceptions.',
    long: 'Every action counts: training, fighting, questing, trading and picking things up. Quests get done in pieces as the circles pass. Nothing is ever locked, so nothing can brick a run. Your circle just has to be in the right place.',
  },
  {
    name: 'Drift',
    short: 'Main roams Gielinor at about 15 tiles an hour. Kingdom circles patrol their kingdom at about 45.',
    long: 'Circles only move while you are logged in, one step per played minute, each on its own slowly wandering heading. The path is fixed by your seed, so nobody, you included, knows where it goes next.',
  },
  {
    name: 'Breathe',
    short: 'Circles slowly grow and shrink, and now and then pulse by 8 tiles.',
    long: "Main's radius ranges from 12 to 48 tiles and starts at 24. A kingdom circle's radius ranges from 12 to 32 and starts at 20.",
  },
  {
    name: 'Cross',
    short: 'Where Main touches a kingdom circle, you may cross into it. The only way out is back into Main.',
    long: 'Stand where the two circles overlap and cross. A kingdom circle never leaves its kingdom. If Main drifts away while you are inside one, you are stranded: your circle leans toward Main and waits at the border until Main comes back.',
  },
  {
    name: 'Shore',
    short: 'Main stays on land and turns back at the coast. It crosses water only by boat.',
    long: 'While a port is inside Main, it may board a quest-free boat to another port. Once you unlock Sailing, Main can sail from any port to any port.',
  },
  {
    name: 'Outside',
    short: 'Step outside and your circle drifts 5 times faster and shrinks a tile a minute.',
    long: 'Leaving is never forbidden, but it costs you. Get back in quickly, or your circle may leave without you.',
  },
  {
    name: 'Below',
    short: 'Underground, upstairs or in an instance, you count as standing where you went in.',
    long: 'Dungeons, upper floors and instances use the surface tile of their entrance. If your circle drifts off that tile while you are down there, you are outside, so keep an eye on the edge.',
  },
];

export const GROWTH = [
  'Each Achievement Diary tier permanently grows that kingdom\'s circle. With every tier done, it ranges from 28 to 48 tiles.',
  'Kingdoms with two diaries (Kandarin with Ardougne, Misthalin with Lumbridge and Varrock) need both to fill. Varlamore has no diary yet, so its circle stays as it is.',
];
