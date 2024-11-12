import React from "react";

import Image from "next/image";
import Link from "next/link";

import { CheckIcon } from "lucide-react";

import bubles from "@/assets/bubles.png";
import { ExternalLink } from "@/components/chromia-ui-kit/icons";
import LinkButton from "@/components/chromia-ui-kit/link-button";

export default function Home() {
  return (
    <>
      <div className="flex flex-col justify-center gap-4 py-8 md:items-center md:text-center lg:py-24">
        <a
          href="#"
          className="relative mb-4 grid h-14 w-[205px] place-items-center bg-success/15 font-bold text-success transition-colors hover:bg-success/20 focus:bg-success/10"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="214"
            height="64"
            viewBox="0 0 214 64"
            className="absolute"
            fill="none"
          >
            <path
              d="M205.5 4H209.5M213.5 4H209.5M209.5 4V8M209.5 4V0"
              stroke="#93F091"
            />
            <path
              d="M205.5 60H209.5M213.5 60H209.5M209.5 60V64M209.5 60V56"
              stroke="#93F091"
            />
            <path d="M0.5 4H4.5M8.5 4H4.5M4.5 4V8M4.5 4V0" stroke="#93F091" />
            <path
              d="M0.5 60H4.5M8.5 60H4.5M4.5 60V64M4.5 60V56"
              stroke="#93F091"
            />
          </svg>
          Open Source Dapp 🚀
        </a>
        <h1 className="text-3xl font-bold lg:text-6xl">
          Use Our Product as a Starting Point
        </h1>
        <p className="text-xl lg:text-2xl">
          Set up your modern crypto app quickly and securely.
          <br />
          All it takes is running one simple command.
        </p>
        <Link href="/token" passHref legacyBehavior>
          <LinkButton className="w-44 lg:mt-4">Get Started</LinkButton>
        </Link>
      </div>
      <div className="mx-auto flex max-w-full flex-col gap-10 pb-12 pt-4 lg:flex-row lg:py-32 xl:gap-28">
        <div className="max-w-lg space-y-8">
          <div className="space-y-4">
            <h2 className="font-serif text-2xl leading-normal lg:text-[2rem]">
              Open-Source Crypto DApp: A Solid Foundation for Developers
            </h2>
            <div className="flex gap-1">
              <CheckIcon className="shrink-0 text-3xl text-accent" />
              <p className="text-muted-foreground">
                <strong className="text-white">
                  Flexible Development Base:
                </strong>
                 Our open-source crypto DApp provides a versatile foundation for
                developers to build their own decentralized applications. It
                comes with essential tools to streamline the development
                process.
              </p>
            </div>
            <div className="flex gap-1">
              <CheckIcon className="shrink-0 text-3xl text-accent" />
              <p className="text-muted-foreground">
                <strong className="text-white">Core Token Features:</strong>
                 The DApp includes functionalities like minting test tokens,
                transferring tokens, and burning tokens. These features allow
                you to focus on enhancing and customizing your application.{" "}
              </p>
            </div>
            <div className="flex gap-1">
              <CheckIcon className="shrink-0 text-3xl text-accent" />
              <p className="text-muted-foreground">
                <strong className="text-white">
                  User Engagement Incentives:
                </strong>
                 Reward users with bonus points (stars) for each successful
                action. This system encourages interaction and adds a layer of
                gamification to your DApp.
              </p>
            </div>
          </div>
          <div className="flex gap-4 lg:gap-10">
            <LinkButton
              href="https://learn.chromia.com/"
              target="_blank"
              rel="noopener noreferrer"
              className="w-44"
              variant="secondary"
            >
              Learn
              <ExternalLink />
            </LinkButton>
            <LinkButton
              href="https://docs.chromia.com/"
              target="_blank"
              rel="noopener noreferrer"
              className="w-44"
              variant="secondary"
            >
              Docs
              <ExternalLink />
            </LinkButton>
          </div>
        </div>
        <Image
          alt="Chromia logo with bubles"
          className="mx-auto"
          src={bubles}
          width={446}
        />
      </div>
    </>
  );
}
