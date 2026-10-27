const REPO = 'https://github.com/Nubles/OSRS-Wandering-Nomad';

export function Plugin() {
  return (
    <article className="page prose">
      <div className="osrs-win window">
      <div className="win-body">
      <p className="osrs-title">RuneLite plugin</p>
      <h1>Play with the plugin</h1>
      <p className="lede">The plugin runs your circles, draws them in the game and keeps the run log. This site uses the same engine.</p>

      <h2>What it shows</h2>
      <ul>
        <li>Your circle's edge on the ground, on the minimap and on the world map.</li>
        <li>A status box: which circle you're bound to, its radius, whether you're inside, and any open crossing.</li>
        <li>A sidebar with crossing buttons, your diary growth, the run log and a button to open your run on this site.</li>
      </ul>

      <h2>Install</h2>
      <p>
        Nomad UIM isn't on the Plugin Hub yet. Until it is, you can build it and run it from source with Java 11 and Gradle:
      </p>
      <pre className="code">{`git clone ${REPO}.git
cd OSRS-Wandering-Nomad
gradle runClient`}</pre>
      <p>
        Each build on GitHub also uploads a ready-made jar under{' '}
        <a href={`${REPO}/actions/workflows/build.yml`} target="_blank" rel="noreferrer">
          Actions
        </a>
        .
      </p>

      <h2>See your run here</h2>
      <p>
        In the plugin's sidebar, press <strong>View on website</strong>. It opens this site with your circles exactly where they are in
        your game. You can also press <strong>Copy run code</strong> and paste the code into the box on the map page. The link holds your
        run, so you can send it to a friend or put it in a Discord channel.
      </p>
      <p>
        A run link is a snapshot of the moment you made it.
      </p>

      <h2>Share it live</h2>
      <p>
        Turn on <strong>Share live on the website</strong> in the plugin's settings, then press <strong>Copy live link</strong>. Anyone
        with the link sees your circles move as you play: it's made for streams and Discord. The plugin sends your run to the Nomad
        relay every few minutes, and within a minute when something happens, like a crossing or a voyage. Between sends the page moves
        the circles itself with the same engine, so they move smoothly.
      </p>
      <p className="muted">
        Live sharing is off until you turn it on. It sends your run and the tile you count as standing on, and nothing else. When you
        log out, the page shows the run as offline.
      </p>
      </div>
      </div>
    </article>
  );
}
